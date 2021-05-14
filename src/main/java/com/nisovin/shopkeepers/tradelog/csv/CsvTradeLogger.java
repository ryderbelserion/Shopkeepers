package com.nisovin.shopkeepers.tradelog.csv;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.tradelog.TradeLogger;
import com.nisovin.shopkeepers.tradelog.data.PlayerRecord;
import com.nisovin.shopkeepers.tradelog.data.ShopRecord;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.FileUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Retry;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.SingletonTask;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.ThrowableUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.VoidCallable;
import com.nisovin.shopkeepers.util.csv.CsvFormatter;
import com.nisovin.shopkeepers.util.yaml.YamlUtils;

/**
 * Logs trades to CSV files.
 */
public class CsvTradeLogger implements TradeLogger {

	private static final String TRADE_LOGS_FOLDER = "trade-logs";
	private static final String FILE_NAME_PREFIX = "trades-";
	private static final List<String> CSV_HEADER = Arrays.asList(
			"time", "player_uuid", "player_name",
			"shop_uuid", "shop_type", "shop_world", "shop_x", "shop_y", "shop_z",
			"shop_owner_uuid", "shop_owner_name",
			"item1_type", "item1_amount", "item1_metadata",
			"item2_type", "item2_amount", "item2_metadata",
			"result_item_type", "result_item_amount", "result_item_metadata"
	);

	// TODO This uses the local locale and timezone currently. Config option(s) to change the locale and timezone? Or
	// always store in UTC?
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int DELAYED_SAVE_TICKS = 100; // 5 seconds

	private static final int SAVE_MAX_ATTEMPTS = 20;
	private static final long SAVE_RETRY_DELAY_MILLIS = 25L;

	private final Plugin plugin;
	private final Path tradeLogsFolder;
	// Note: Even though the CSV format allows quoted fields to span across multiple lines, we want each CSV record to
	// only span a single line. However, even though we do not want fields to contain unescaped newlines, we do not
	// escape these newlines via the CSV formatter. Instead, we expect that any data that we pass to the CSV formatter
	// has its newlines already escaped. This ensures that we do not redundantly escape backslashes twice. Instead, the
	// CSV formatter will log a warning if it encounters newlines in the data nevertheless.
	private final CsvFormatter csv = new CsvFormatter()
			.escapeNewlines(false)
			.warnOnNewlines();
	private List<TradeRecord> pending = new ArrayList<>();
	private final SaveTask saveTask;
	private BukkitTask delayedSaveTask = null;
	// This is reset to the current configuration value prior to every save. This ensures that the value of this setting
	// remains constant during the save and does not differ for the items of the trades that are being saved as part of
	// the same batch.
	private boolean logItemMetadata;

	public CsvTradeLogger(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.tradeLogsFolder = plugin.getDataFolder().toPath().resolve(TRADE_LOGS_FOLDER);
		this.saveTask = new SaveTask(plugin);
	}

	@Override
	public void logTrade(TradeRecord trade) {
		pending.add(trade);

		// We do not trigger a save right away, because it is likely for there to be more trades to log in the immediate
		// future:
		this.savePendingDelayed();
	}

	@Override
	public void flush() {
		this.savePending();
		saveTask.awaitExecutions();
	}

	private boolean isDirty() {
		return !pending.isEmpty();
	}

	private void savePendingDelayed() {
		if (!this.isDirty()) {
			// There are no pending trades to save:
			return;
		}
		if (delayedSaveTask != null) {
			// There is already a delayed save in progress:
			return;
		}

		delayedSaveTask = SchedulerUtils.runTaskLaterOrOmit(plugin, () -> {
			delayedSaveTask = null;
			this.savePending();
		}, DELAYED_SAVE_TICKS);
	}

	private void cancelDelayedSave() {
		if (delayedSaveTask != null) {
			delayedSaveTask.cancel();
			delayedSaveTask = null;
		}
	}

	private void savePending() {
		if (!this.isDirty()) {
			// There are no pending trades to save:
			return;
		}
		saveTask.run(); // Usually async, but may be sync during plugin disable
	}

	private class SaveTask extends SingletonTask {

		private List<TradeRecord> saving = new ArrayList<>();
		private SaveContext saveContext = null;
		private boolean saveSucceeded = false;
		private long lastSaveErrorMsgTimestamp = 0L;

		SaveTask(Plugin plugin) {
			super(plugin);
		}

		@Override
		protected void prepare() {
			// Stop any active delayed save task:
			cancelDelayedSave();

			// Reset local logItemMetadata setting:
			logItemMetadata = Settings.logItemMetadata;

			// Swap the pending and saving lists of trades:
			assert saving.isEmpty();
			List<TradeRecord> temp = saving;
			saving = pending;
			pending = temp;

			// Setup new SaveContext:
			assert saveContext == null;
			saveContext = new SaveContext(saving);
		}

		@Override
		protected void execute() {
			saveSucceeded = writeTradesToDisk(saveContext);
			assert saveSucceeded ? !saveContext.hasUnsavedTrades() : saveContext.hasUnsavedTrades();
		}

		@Override
		protected void syncCallback() {
			this.printDebugInfo();

			if (!saveSucceeded) {
				// Save failed:

				// Add the unsaved trades to the front of the pending trades:
				pending.addAll(0, saveContext.getUnsavedTrades());

				// Attempt the save again after a short delay:
				// However, during the final save attempt during plugin disable, this is skipped and data might be lost.
				savePendingDelayed();

				// Inform admins about the issue (throttled to once every 5 minutes):
				if (Math.abs(System.currentTimeMillis() - lastSaveErrorMsgTimestamp) > (5 * 60 * 1000L)) {
					lastSaveErrorMsgTimestamp = System.currentTimeMillis();
					String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED + "Logging trades to the CSV trade log failed!"
							+ " Please check the server logs and look into the issue!";
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player.hasPermission(ShopkeepersPlugin.ADMIN_PERMISSION)) {
							player.sendMessage(errorMsg);
						}
					}
				}
			}

			// Reset:
			saveContext = null;
			saving.clear();
		}

		private void printDebugInfo() {
			Log.debug(() -> {
				StringBuilder sb = new StringBuilder();
				sb.append("Logged trades to the CSV trade log (");

				// Number of logged trades:
				sb.append(saving.size()).append(" trades");

				// Number of trades that we failed to log:
				if (saveContext.hasUnsavedTrades()) {
					sb.append(", ").append(saveContext.getUnsavedTrades().size()).append(" failed to log");
				}

				// Timing summary:
				sb.append("): ");
				sb.append(this.getExecutionTimingString());

				// Failure indicator:
				if (!saveSucceeded) {
					if (saveContext.getUnsavedTrades().size() == saving.size()) {
						sb.append(" -- Logging failed!");
					} else {
						sb.append(" -- Logging partially failed!");
					}
				}
				return sb.toString();
			});
		}
	}

	private static class SaveContext {

		private final List<TradeRecord> trades;
		private int nextUnsaved = 0;

		SaveContext(List<TradeRecord> trades) {
			assert trades != null && !trades.contains(null);
			this.trades = trades;
		}

		public boolean hasUnsavedTrades() {
			return (nextUnsaved < trades.size());
		}

		// Returns null if there are no more unsaved trades.
		// Does not move the cursor forward until onTradeSuccessfullySaved() has been called.
		public TradeRecord getNextUnsavedTrade() {
			if (!this.hasUnsavedTrades()) return null;
			return trades.get(nextUnsaved);
		}

		// May return a sublist view:
		public List<TradeRecord> getUnsavedTrades() {
			if (!this.hasUnsavedTrades()) {
				return Collections.emptyList();
			} else {
				return trades.subList(nextUnsaved, trades.size());
			}
		}

		public void onTradeSuccessfullySaved() {
			nextUnsaved++;
		}
	}

	private Path getLogFile(Instant timestamp) {
		assert timestamp != null;
		return tradeLogsFolder.resolve(FILE_NAME_PREFIX + DATE_FORMAT.format(timestamp) + ".csv");
	}

	// Note: We log the item metadata in Yaml format. Since this is what Bukkit natively supports for serializing and
	// deserializing ItemStacks, this ensures that we are able to load the data again and recreate the original
	// ItemStack (if we ever wish to).
	// An alternative would be to log it in Json format, which may have better library support across languages.
	// However, Gson (the Json library included with the Minecraft server and Bukkit) will not properly preserve certain
	// data types by default (at least not if we don't provide detailed custom deserializers for every type of data that
	// we may want to deserialize, or a deserializer that replicates Yaml's parsing of certain primitive types, which is
	// actually not that easily possible): For instance, if the numeric data type of a loaded Json number is unknown,
	// Gson loads it as a double by default (without there being an easy way to change that). But since some parts of
	// Bukkit's ItemStack deserialization have strict expectations regarding the type of data to deserialize, the
	// deserialization from Json may fail for this data.
	private String getItemMetadata(ItemStack itemStack) {
		assert itemStack != null;
		if (!logItemMetadata) return ""; // Disabled

		// If the logging of item metadata is enabled, we not only store the item's ItemMeta (if it has any), but also
		// its data version. We therefore serialize the complete ItemStack here, but then remove the item's type and
		// amount again, since these properties are already getting stored separately.
		Map<String, Object> itemData = itemStack.serialize(); // Assert: The returned Map is modifiable.
		itemData.remove("type");
		itemData.remove("amount");
		// In order to ensure single-line CSV records, we format the Yaml compactly:
		String yaml = YamlUtils.toCompactYaml(itemData);
		return yaml;
	}

	private String toCSVRecord(TradeRecord trade) {
		Instant timestamp = trade.getTimestamp();
		PlayerRecord player = trade.getPlayer();

		ShopRecord shop = trade.getShop();
		String worldName = StringUtils.getOrEmpty(shop.getWorldName());
		PlayerRecord shopOwner = shop.getOwner();
		String shopOwnerId = "";
		String shopOwnerName = "";
		if (shopOwner != null) {
			shopOwnerId = shopOwner.getUniqueId().toString();
			shopOwnerName = shopOwner.getName();
		}

		ItemStack resultItem = trade.getResultItem();
		ItemStack item1 = trade.getItem1();
		ItemStack item2 = trade.getItem2(); // Can be null
		String item2Type = "";
		String item2Amount = "";
		String item2Metadata = "";
		if (item2 != null) {
			item2Type = item2.getType().name();
			item2Amount = String.valueOf(item2.getAmount());
			item2Metadata = this.getItemMetadata(item2);
		}

		// "time", "player_uuid", "player_name",
		// "shop_uuid", "shop_type", "shop_world", "shop_x", "shop_y", "shop_z",
		// "shop_owner_uuid", "shop_owner_name",
		// "item1_type", "item1_amount", "item1_metadata",
		// "item2_type", "item2_amount", "item2_metadata",
		// "result_item_type", "result_item_amount", "result_item_metadata"
		return csv.formatRecord(Arrays.asList(
				TIME_FORMAT.format(timestamp), player.getUniqueId(), player.getName(),
				shop.getUniqueId(), shop.getTypeId(), worldName, shop.getX(), shop.getY(), shop.getZ(),
				shopOwnerId, shopOwnerName,
				item1.getType().name(), item1.getAmount(), this.getItemMetadata(item1),
				item2Type, item2Amount, item2Metadata,
				resultItem.getType().name(), resultItem.getAmount(), this.getItemMetadata(resultItem)

		));
	}

	// May be invoked asynchronously.
	// Returns true on success.
	public boolean writeTradesToDisk(SaveContext saveContext) {
		try {
			Retry.retry((VoidCallable) () -> {
				this.writeTradesToLogFile(saveContext);
			}, SAVE_MAX_ATTEMPTS, (attemptNumber, exception, retry) -> {
				// Trade logging failed. Log a compact description of the issue:
				assert exception != null;
				String issue = ThrowableUtils.getDescription(exception);
				Log.severe("Failed to log trades to the CSV trade log (attempt " + attemptNumber + "): " + issue);

				// In order to not spam with errors and stacktraces, we only print them once for the first failed saving
				// attempt, and again for the last failed attempt:
				if (attemptNumber == 1) {
					exception.printStackTrace();
				}

				// Try again after a small delay:
				if (retry) {
					try {
						Thread.sleep(SAVE_RETRY_DELAY_MILLIS);
					} catch (InterruptedException e) {
					}
				}
			});
			return true;
		} catch (Exception e) {
			Log.severe("Failed to log trades to the CSV trade log! Data might have been lost! :(", e);
			return false;
		}
	}

	/*
	 * Goals:
	 * - Reliably log all trades.
	 * - Log trades in the order in which they occurred.
	 * - Log trades atomically, i.e. not partially, intertwined, or duplicated (eg. if we retry failed log attempts).
	 * 
	 * Measures:
	 * - We write to the log files via a single thread only, and assume that no other processes write to them
	 *   (concurrent reads should not be an issue).
	 * - We use synchronous IO to ensure that each trade is actually persisted to the storage before we assume it
	 *   to have been successfully logged.
	 * - If the logging of a trade fails for some reasons, we retry it until it succeeds. However, for this to not
	 *   result in trades being partially logged, or logged multiple times, the logging has to be atomic. I.e. it has
	 *   to either succeed completely, or fail completely. See the notes on that below.
	 * - We assume that appending to the log file is atomic, i.e. that each trade that is logged via a single write
	 *   is either successfully logged completely, or not at all. In practice, depending on the OS and file system,
	 *   this may only apply for writes that are smaller than a certain threshold.
	 * - In order to keep each write small, and therefore hopefully atomic, we use an unbuffered writer and a single
	 *   write call to log each trade. Also, using a buffer would incur the risk of a write that is meant to be atomic
	 *   (i.e. a trade that is being logged) to actually be split across multiple writes to the underlying file, and
	 *   then no longer be atomic.
	 * 
	 * References regarding atomicity of file appends:
	 * - https://www.notthewizard.com/2014/06/17/are-files-appends-really-atomic/
	 * - https://nblumhardt.com/2016/08/atomic-shared-log-file-writes/
	 */
	// May be invoked asynchronously.
	// Depending on their timestamps, the trades may need to be logged to different log files. This writes all
	// consecutive trades that need to be logged to the same log file, and then recursively invokes itself to write the
	// remaining trades to other log files.
	private void writeTradesToLogFile(SaveContext saveContext) throws IOException {
		TradeRecord trade = saveContext.getNextUnsavedTrade();
		if (trade == null) return; // There are no unsaved trades

		Path logFile = this.getLogFile(trade.getTimestamp());

		// Create the parent directories if they are missing:
		FileUtils.createParentDirectories(logFile);

		// Check the write permission for the parent directory:
		FileUtils.checkIsDirectoryWritable(logFile.getParent());

		// Check if the file already exists:
		boolean isNew = !Files.exists(logFile);
		// Check if the file is empty: This may for example occur if we were able to create the file during a previous
		// log attempt, but then failed to write to it.
		boolean isEmpty = (isNew || Files.size(logFile) == 0L);

		// Check the write permission for the log file, if it already exists:
		if (!isNew) {
			FileUtils.checkIsFileWritable(logFile);
		}

		OpenOption[] openOptions;
		if (isNew) {
			// Create the new file, but fail if the assumption that the file does not yet exist turns out to no longer
			// hold when we actually attempt to create the file:
			openOptions = new OpenOption[] {
				StandardOpenOption.CREATE_NEW, // Create new file, fail if it already exists
				StandardOpenOption.WRITE, // Open for write access
				StandardOpenOption.APPEND, // Append to the end of the file
				StandardOpenOption.DSYNC // Ensure that each write is persisted to storage before we continue
			};
		} else {
			// Fails if the file no longer exists when the attempt to open it:
			openOptions = new OpenOption[] {
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND,
				StandardOpenOption.DSYNC
			};
			// Note: Opening the file for writing will also fail if the file is actually a directory instead of a
			// regular file.
		}

		// TODO Use the file encoding specified inside the config? Or add a separate setting?
		boolean done = false;
		try (Writer writer = FileUtils.newUnbufferedWriter(logFile, StandardCharsets.UTF_8, openOptions)) {
			// Even though we use an unbuffered writer, we flush after every write just in case, and to make our intent
			// more clear.
			// TODO What if we receive an IOException during a flush? Has the trade been logged or not at that point?

			if (isNew) {
				// Fsync the parent directory to ensure that the newly created log file has been successfully persisted.
				// We do this prior to writing to the new file, so that we can be sure that nothing has been written to
				// the file yet if this operation fails for some reason.
				FileUtils.fsyncParentDirectory(logFile);
			}

			// If the file is new or empty, write the CSV header:
			if (isEmpty) {
				// Note: A BOM should not be required for UTF-8, and it is actually recommended to omit it.
				writer.write(csv.formatRecord(CSV_HEADER));
				writer.flush();
			}

			// Instead of closing and reopening the log file for each trade, we log all consecutive trades that need to
			// be logged to the same log file before we close it again:
			do {
				// Write the new trade record:
				writer.write(this.toCSVRecord(trade));
				writer.flush();

				// If we did not throw an IOException up until this point, we assume that the trade has been
				// successfully written to the trade log.
				saveContext.onTradeSuccessfullySaved();

				// Get the next trade to save:
				trade = saveContext.getNextUnsavedTrade();
				if (trade == null) break; // There are no more trades to save

				Path nextLogFile = this.getLogFile(trade.getTimestamp());
				if (!logFile.equals(nextLogFile)) {
					break;
				} // Else: Continue.
			} while (true);

			// We are about to close the log file:
			done = true;
		} catch (IOException e) {
			if (!done) {
				throw e;
			} else {
				// Since the previous writes reported to have been successful, we assume that the trades have been
				// successfully logged. We therefore ignore any exceptions raised during the closing of the writer: They
				// are still logged, but they don't trigger a retry of the trade log attempt.
				Log.severe("Failed to close the CSV trade log file!", e);
			}
		}

		// Recursively log the remaining trades to their target log files:
		if (saveContext.hasUnsavedTrades()) {
			this.writeTradesToLogFile(saveContext);
		}
	}
}
