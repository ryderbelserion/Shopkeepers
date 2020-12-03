package com.nisovin.shopkeepers.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.StringUtils;

/**
 * Storage responsible for the shopkeepers data.
 * <p>
 * Implementation notes:
 * <ul>
 * <li>There can at most be one thread doing file IO at the same time.
 * <li>Saving preparation always happens on the server's main thread. There can at most be one save getting prepared at
 * the same time.
 * <li>If there is a request for an <b>async</b> save while an async save is already in progress, a flag gets set to
 * indicate this after the current async save is finished.
 * <li>If there is a request for a <b>sync</b> save while an async save is already in progress, the main thread waits
 * for the async save to finish (or aborts it), before preparing the next save.
 * </ul>
 */
public class SKShopkeeperStorage implements ShopkeeperStorage {

	private static final String DATA_FOLDER = "data";

	// Our stored 'data version' is a combination of two different data versions:
	// - Our own 'shopkeepers data version', which we can use to determine our own required migrations or force a full
	// save of all shopkeepers data after we have made changes to the storage format which affect all shopkeepers.
	// - Minecraft's data version, which updates with every server update (even minor updates). Since these updates
	// sometimes indicate item migrations done by Minecraft, they are also relevant for our stored item data.

	// Our goal is to always keep the item data stored within the save.yml file up-to-date with the current server
	// version to avoid ending up with very old items inside our save data that never got updated. For that reason we
	// always trigger a full save of all shopkeepers data whenever one of the above mentioned data versions has changed.
	// The stored and compared data version is a simple concatenation of these two data versions.

	private static final int SHOPKEEPERS_DATA_VERSION = 2;
	private static final String MISSING_DATA_VERSION = "-";
	private static final String DATA_VERSION_KEY = "data-version";

	private static final String HEADER = "This file is not intended to be manually modified! If you want to manually edit this"
			+ " file anyways, ensure that the server is not running currently and that you have prepared a backup of this file.";

	private final SKShopkeepersPlugin plugin;
	private final int minecraftDataVersion;
	private final DataVersion currentDataVersion;

	/*
	 * Holds the data that gets used by the current/next (possibly async) save task.
	 * This also contains any data of shopkeepers that could not be loaded correctly.
	 * This cannot be modified while an async save is in progress.
	 */
	private final FileConfiguration saveData = new YamlConfiguration();
	private int maxStoredShopkeeperId = 0;
	private int nextShopkeeperId = 1;

	// Flag to (temporary) turn off saving:
	private boolean savingDisabled = false;
	private long lastSavingErrorMsgTimestamp = 0L;
	// There might be shopkeepers with unsaved data, or we got an explicit save request:
	private boolean dirty = false;
	private int delayedSaveTaskId = -1;

	// Current loading:
	private boolean currentlyLoading = false;

	// Current saving:
	// Indicates if we are currently processing a save (gets reset to false once the save has been processed on the main
	// thread, or handed over to the async IO task):
	private boolean currentlyProcessingSave = false;
	private final SaveResult saveResult = new SaveResult();
	// Previously dirty shopkeepers which we currently attempt to save:
	private final List<AbstractShopkeeper> savingShopkeepers = new ArrayList<>();
	// The task which performs async file io during a save:
	private int saveIOTask = -1;
	// The saving callback of the current save: may need to be run manually during plugin disable or save abortion
	private Runnable syncSavingCallback = null;
	// Whether there was an abort request for the last async save:
	private boolean abortSave = false;
	// Determines if there was another saveReal-request while the saveIOTask was still in progress
	private boolean saveAgain = false;
	// Shopkeepers that got deleted during the last async save:
	private final List<AbstractShopkeeper> shopkeepersToDelete = new ArrayList<>();
	// Number of shopkeepers whose data got removed since the last save:
	private int deletedShopkeepersCount = 0;

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.minecraftDataVersion = this.getMinecraftDataVersion();
		this.currentDataVersion = new DataVersion(SHOPKEEPERS_DATA_VERSION, minecraftDataVersion);
	}

	private int getMinecraftDataVersion() {
		try {
			return Bukkit.getUnsafe().getDataVersion();
		} catch (Exception e) {
			Log.warning("Could not determine Minecraft's current data version!", e);
			return 0;
		}
	}

	public void onEnable() {
		// Start save task:
		if (!Settings.saveInstantly) {
			this.startSaveTask();
		}
	}

	public void onDisable() {
		// Reset a few things:
		this.clearSaveData();
		savingShopkeepers.clear();
		savingDisabled = false;
		lastSavingErrorMsgTimestamp = 0L;
		dirty = false;
		delayedSaveTaskId = -1;
		saveIOTask = -1;
		syncSavingCallback = null;
		abortSave = false;
		saveAgain = false;
		shopkeepersToDelete.clear();
		deletedShopkeepersCount = 0;
	}

	private void startSaveTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (this.isDirty()) {
				this.saveNow();
			}
		}, 6000, 6000); // 5 minutes
	}

	private SKShopkeeperRegistry getShopkeeperRegistry() {
		return plugin.getShopkeeperRegistry();
	}

	public int getDirtyCount() {
		int dirtyShopkeepersCount = 0;
		for (AbstractShopkeeper shopkeeper : this.getShopkeeperRegistry().getAllShopkeepers()) {
			if (shopkeeper.isDirty()) {
				dirtyShopkeepersCount++;
			}
		}
		return dirtyShopkeepersCount;
	}

	public int getUnsavedDeletedCount() {
		return deletedShopkeepersCount;
	}

	public void disableSaving() {
		this.savingDisabled = true;
	}

	public void enableSaving() {
		this.savingDisabled = false;
	}

	private File getDataFolder() {
		return new File(plugin.getDataFolder(), DATA_FOLDER);
	}

	private File getSaveFile() {
		return new File(this.getDataFolder(), "save.yml");
	}

	private File getTempSaveFile() {
		File saveFile = this.getSaveFile();
		return new File(saveFile.getParentFile(), saveFile.getName() + ".temp");
	}

	// SHOPKEEPER IDs

	// Does not increment the shopkeeper id counter on its own (we don't want to increment it in case the shopkeeper
	// creation fails).
	public int getNextShopkeeperId() {
		int nextId = nextShopkeeperId; // Can end up negative after increments due to overflows
		if (nextId <= 0 || !this.isUnusedId(nextId)) {
			// Try to use an id larger than the max currently used id:
			int maxId = maxStoredShopkeeperId;
			for (Shopkeeper shopkeeper : this.getShopkeeperRegistry().getAllShopkeepers()) {
				int id = shopkeeper.getId();
				if (id > maxId) {
					maxId = id;
				}
			}
			assert maxId > 0;
			if (maxId < Integer.MAX_VALUE) {
				nextId = maxId + 1;
			} else {
				// Find the first unused id:
				nextId = 1;
				while (!this.isUnusedId(nextId)) {
					nextId++;
					if (nextId <= 0) {
						// Overflow, all ids are in use..:
						throw new IllegalStateException("No unused shopkeeper ids available!");
					}
				}
				assert nextId > 0;
			}
			// Remember the found next id:
			nextShopkeeperId = nextId;
		}
		return nextId;
	}

	// Also takes ids of stored shopkeepers in account that couldn't be loaded for some reason.
	private boolean isUnusedId(int id) {
		return (!saveData.contains(String.valueOf(id)) && this.getShopkeeperRegistry().getShopkeeperById(id) == null);
	}

	public void onShopkeeperIdUsed(int id) {
		if (id >= nextShopkeeperId) nextShopkeeperId = id + 1;
	}

	// SHOPKEEPER DATA REMOVAL

	/**
	 * Clears and sets up the empty save data file configuration.
	 */
	private void clearSaveData() {
		ConfigUtils.clearConfigSection(saveData);
		maxStoredShopkeeperId = 0;
		nextShopkeeperId = 1;

		// Setup data version as first / top entry:
		// Explicitly setting the 'missing data version' value here ensures that the data version will be the first
		// entry in the save file, even if it is missing in the actual file currently (without having to move all loaded
		// shopkeeper entries around later).
		// It gets replaced with the actual data version during loading.
		saveData.set(DATA_VERSION_KEY, MISSING_DATA_VERSION);
	}

	public void clearShopkeeperData(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (this.isCurrentlySavingAsync()) {
			// Remember to remove the data after the current async save has finished:
			shopkeepersToDelete.add(shopkeeper);
		} else {
			String key = String.valueOf(shopkeeper.getId());
			saveData.set(key, null);
			deletedShopkeepersCount++;
		}
	}

	// LOADING

	// We previously stored the save file within the plugin's root folder. If no save file exist at the expected
	// location, we check the old save file location and migrate the save file if it is found.
	private File getOldSaveFile() {
		return new File(plugin.getDataFolder(), "save.yml");
	}

	private File getOldTempSaveFile() {
		File saveFile = this.getOldSaveFile();
		return new File(saveFile.getParentFile(), saveFile.getName() + ".temp");
	}

	// Returns false if the migration failed.
	// Returns true if the migration succeeded or there is no old save file to migrate.
	private boolean migrateOldSaveFile() {
		File oldSaveFile = this.getOldSaveFile();
		if (!oldSaveFile.exists()) {
			File oldTempSaveFile = this.getOldTempSaveFile();
			if (oldTempSaveFile.exists()) {
				// Load from old temporary save file instead:
				Log.warning("Found no old save file, but an existing old temporary save file! (" + oldTempSaveFile.getName() + ")");
				Log.warning("This might indicate an issue during a previous saving attempt!");
				Log.warning("We try to migrate this temporary save file instead!");

				oldSaveFile = oldTempSaveFile;
			} else {
				// No save file found that needs to be migrated.
				return true;
			}
		}

		// Move old save file to new location:
		File saveFile = this.getSaveFile();
		Log.info("Migrating old save file (" + oldSaveFile.getName() + ") to new location ("
				+ saveFile.getParentFile().getName() + "/" + saveFile.getName() + ")!");
		try {
			Files.move(oldSaveFile.toPath(), saveFile.toPath());
		} catch (IOException e) {
			Log.severe("Failed to migrate old save file! (" + oldSaveFile.getName() + ")", e);
			return false;
		}
		// Migration succeeded:
		return true;
	}

	// Returns true on success, and false if there was some severe issue during loading.
	public boolean reload() {
		if (currentlyLoading) {
			throw new IllegalStateException("Already loading right now!");
		}
		currentlyLoading = true;
		boolean result;

		// No concurrent access of the save file:
		synchronized (SAVING_IO_LOCK) {
			try {
				result = this.doReload();
			} catch (Exception e) {
				Log.severe("Something completely unexpected went wrong during the loading of the saved shopkeepers data!", e);
				result = false; // Error
			}
		}
		currentlyLoading = false;
		return result;
	}

	// Returns true on success, and false if there was some severe issue during loading.
	private boolean doReload() {
		// Unload all currently loaded shopkeepers:
		SKShopkeeperRegistry shopkeeperRegistry = this.getShopkeeperRegistry();
		shopkeeperRegistry.unloadAllShopkeepers();
		this.clearSaveData();

		File saveFile = this.getSaveFile();
		if (!saveFile.exists()) {
			File tempSaveFile = this.getTempSaveFile();
			if (tempSaveFile.exists()) {
				// Load from temporary save file instead:
				Log.warning("Found no save file, but an existing temporary save file! (" + tempSaveFile.getName() + ")");
				Log.warning("This might indicate an issue during a previous saving attempt!");
				Log.warning("We try to load the Shopkeepers data from this temporary save file instead!");

				saveFile = tempSaveFile;
			} else if (!this.migrateOldSaveFile()) {
				// Migration of old save file failed:
				return false; // Disable without save
			} else if (!saveFile.exists()) {
				// No save file exists yet (even after checking for it again, after the migration) -> No shopkeeper data
				// available.
				// We silently setup the data version and abort:
				saveData.set(DATA_VERSION_KEY, currentDataVersion.getCombinded());
				return true;
			}
		}

		try {
			if (!StringUtils.isEmpty(Settings.fileEncoding)) {
				// Load with specified charset:
				try (	FileInputStream stream = new FileInputStream(saveFile);
						InputStreamReader reader = new InputStreamReader(stream, Settings.fileEncoding)) {
					saveData.load(reader);
				}
			} else {
				// Load with default charset handling:
				saveData.load(saveFile);
			}
		} catch (Exception e) {
			Log.severe("Failed to load save file!", e);
			return false; // Disable without save
		}

		Set<String> keys = saveData.getKeys(false);
		// Contains at least the (missing) data-version entry:
		assert keys.contains(DATA_VERSION_KEY);
		int shopkeepersCount = (keys.size() - 1);
		if (shopkeepersCount == 0) {
			// No shopkeeper data exists yet. Silently setup/update data version and abort:
			saveData.set(DATA_VERSION_KEY, currentDataVersion.getCombinded());
			return true;
		}

		Log.info("Loading data of " + shopkeepersCount + " shopkeepers..");
		String dataVersion = saveData.getString(DATA_VERSION_KEY, MISSING_DATA_VERSION);
		boolean dataVersionChanged = (!currentDataVersion.getCombinded().equals(dataVersion));
		if (dataVersionChanged) {
			Log.info("The data version has changed from '" + dataVersion + "' to '" + currentDataVersion.getCombinded()
					+ "': We update the saved data for all loaded shopkeepers.");
			// Update the data version:
			saveData.set(DATA_VERSION_KEY, currentDataVersion.getCombinded());
		}

		for (String key : keys) {
			if (key.equals(DATA_VERSION_KEY)) continue;

			Integer idInt = ConversionUtils.parseInt(key);
			if (idInt == null || idInt <= 0) {
				Log.warning("Failed to load shopkeeper '" + key + "': Invalid id: " + key);
				continue;
			}
			int id = idInt.intValue();
			if (id > maxStoredShopkeeperId) {
				maxStoredShopkeeperId = id;
			}

			ConfigurationSection shopkeeperSection = saveData.getConfigurationSection(key);
			if (shopkeeperSection == null) {
				Log.warning("Failed to load shopkeeper '" + key + "': Invalid config section!");
				continue; // Skip this shopkeeper
			}

			// Perform common migrations:
			MigrationResult migrationResult = this.migrateShopkeeperData(id, shopkeeperSection, dataVersion);
			if (migrationResult == MigrationResult.FAILED) {
				// Migration failed, skip this shopkeeper
				continue;
			}

			String shopTypeString = shopkeeperSection.getString("type");
			AbstractShopType<?> shopType = plugin.getShopTypeRegistry().get(shopTypeString);
			if (shopType == null) {
				Log.warning("Failed to load shopkeeper '" + key + "': Unknown shop type: " + shopTypeString);
				continue; // Skip this shopkeeper
			}

			// Load shopkeeper:
			AbstractShopkeeper shopkeeper;
			try {
				shopkeeper = shopkeeperRegistry.loadShopkeeper(shopType, id, shopkeeperSection);
				assert shopkeeper != null && shopkeeper.isValid();
			} catch (ShopkeeperCreateException e) {
				Log.warning("Failed to load shopkeeper '" + key + "': " + e.getMessage());
				continue; // Skip this shopkeeper
			} catch (Exception e) {
				Log.warning("Failed to load shopkeeper '" + key + "'", e);
				continue; // Skip this shopkeeper
			}

			// If the shopkeeper got migrated or the data version has changed, mark as dirty:
			if (migrationResult == MigrationResult.MIGRATED || dataVersionChanged) {
				shopkeeper.markDirty();
			}
		}
		return true;
	}

	private enum MigrationResult {
		NOTHING_MIGRATED,
		MIGRATED,
		FAILED,
	}

	// Validates and performs migration of the save data.
	private MigrationResult migrateShopkeeperData(int id, ConfigurationSection shopkeeperSection, String dataVersion) {
		MigrationResult migrationResult = MigrationResult.NOTHING_MIGRATED;

		// Convert legacy shop type identifiers:
		String shopTypeString = shopkeeperSection.getString("type");
		if (shopTypeString != null && shopTypeString.equalsIgnoreCase("player")) {
			Log.info("Migrating type of shopkeeper '" + id + "' from 'player' to 'sell'.");
			shopkeeperSection.set("type", "sell");
			migrationResult = MigrationResult.MIGRATED;
		}

		return migrationResult;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
	}

	@Override
	public void save() {
		if (Settings.saveInstantly) {
			this.saveNow();
		} else {
			this.markDirty();
		}
	}

	@Override
	public void saveDelayed() {
		this.markDirty();
		if (Settings.saveInstantly && delayedSaveTaskId == -1) {
			delayedSaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
				if (this.isDirty()) {
					this.saveNow();
				}
				delayedSaveTaskId = -1;
			}, 600).getTaskId(); // 30 seconds delay
		}
	}

	@Override
	public void saveNow() {
		this.saveReal(true);
	}

	@Override
	public void saveImmediate() {
		this.saveReal(false);
	}

	public void saveImmediateIfDirty() {
		// Wait for any async saving to finish:
		this.waitOrAbortAsyncSave();

		// Save if dirty:
		if (this.isDirty()) {
			this.saveImmediate(); // Not async here
		}
	}

	private boolean isCurrentlySavingAsync() {
		return (saveIOTask != -1);
	}

	// Gets run from the main thread.
	// Makes sure that after this method returns there is no saving going on anymore.
	// If an async save has been scheduled already, but not started yet, the save will get aborted,
	// otherwise this will wait for the async save to finish.
	// In either case there might be unsaved data or unhandled save requests.
	private void waitOrAbortAsyncSave() {
		assert Bukkit.isPrimaryThread();

		// If the async task has not yet been passed to the executor (its still pending), we cannot wait here for it to
		// finish (because the scheduler operates on the main thread as well), instead we cancel it:
		if (this.isCurrentlySavingAsync()) {
			Bukkit.getScheduler().cancelTask(saveIOTask);
		}

		// If the saving is currently in progress, acquiring the lock will wait for it to finish:
		synchronized (SAVING_IO_LOCK) {
			// If the task has already been started (there is a worker thread for it already) but not taken the lock
			// yet, we cannot cancel it and need to give up the lock again in order for it to be able to finish.
			// We may be able to request a quicker abort in this case. And if saving has already finished and only the
			// syncSavingCallback is still remaining to get run, this flag signalizes that we don't want any new saving
			// requests (needs to be synchronized here to get correctly propagated):
			abortSave = true;
			while (saveResult.state == SaveResult.State.NOT_YET_STARTED && Bukkit.getScheduler().isCurrentlyRunning(saveIOTask)) {
				try {
					// Release the lock, for the async task to be able to operate.
					// The async task has to notify us once it has finished.
					SAVING_IO_LOCK.wait();
				} catch (InterruptedException e) {
					// We are not interruptible, but we restore the interruption status:
					Thread.currentThread().interrupt();
				}
			}
			// At this point the async task was successfully cancelled or has finished execution.
		}

		// Manually run the callback of the previous save here, to complete the previous save before continuing:
		if (syncSavingCallback != null) {
			syncSavingCallback.run();
		}

		// Reset abort flag:
		abortSave = false;
	}

	private void saveReal(boolean async) {
		if (savingDisabled) {
			Log.warning("Skipping save, because saving got disabled.");
			return;
		}

		if (currentlyProcessingSave) {
			Log.warning("Ignoring saving reqest: We are already processing a save right now!");
			return;
		}
		currentlyProcessingSave = true;

		// Dtop current delayed save task:
		if (delayedSaveTaskId != -1) {
			Bukkit.getScheduler().cancelTask(delayedSaveTaskId);
			delayedSaveTaskId = -1;
		}

		// Is another async save task already running?
		if (this.isCurrentlySavingAsync()) {
			if (async) {
				// Set flag which triggers a new save once that current task is done:
				saveAgain = true;
				currentlyProcessingSave = false;
				return;
			} else {
				// Wait for any async saving to finish:
				this.waitOrAbortAsyncSave();
			}
		}

		// Keep track of statistics and information about this saving attempt:
		saveResult.reset();
		saveResult.async = async;
		saveResult.startTime = System.currentTimeMillis();

		// Setup the file header:
		// This replaces any previously existing and loaded header and thereby ensures that it is always up-to-date
		// after we have saved the file.
		saveData.options().header(HEADER);

		// Store data of dirty shopkeepers into memory configuration:
		saveResult.dirtyShopkeepersCount = 0;
		for (AbstractShopkeeper shopkeeper : this.getShopkeeperRegistry().getAllShopkeepers()) {
			if (!shopkeeper.isDirty()) {
				continue; // Assume storage data is still up-to-date
			}
			saveResult.dirtyShopkeepersCount++;

			String sectionKey = String.valueOf(shopkeeper.getId());
			Object previousData = saveData.get(sectionKey);
			ConfigurationSection newSection = saveData.createSection(sectionKey); // Replaces the previous section
			try {
				shopkeeper.save(newSection);
			} catch (Exception e) {
				// Error while saving shopkeeper data:
				// Restore previous shopkeeper data and then skip this shopkeeper.
				saveData.set(sectionKey, previousData);
				Log.warning("Couldn't save shopkeeper '" + shopkeeper.getId() + "'!", e);
				// The shopkeeper stays marked as dirty, so we attempt to save it again the next time we save all shops.
				// However, we won't automatically initiate a new save for this shopkeeper as the risk is high that
				// saving might fail again anyways.
				continue;
			}

			savingShopkeepers.add(shopkeeper);
			shopkeeper.onSave();
		}

		// Store number of deleted shopkeepers (for debugging purposes):
		saveResult.deletedShopkeepersCount = deletedShopkeepersCount;
		deletedShopkeepersCount = 0;

		// Time to store shopkeeper data in memory configuration:
		saveResult.packingDuration = System.currentTimeMillis() - saveResult.startTime;

		// Note: The dirty flag might get reverted again after saving, if saving failed.
		// However, the flag gets reset here (and not just after successful saving), so that any saving requests that
		// arrive in the meantime get noticed and can cause another save later:
		dirty = false;

		// Gets run on the main thread after the save has been completed or aborted (counts as failure):
		// Note: This needs to be a new runnable (cannot be a lambda), in order to be able to reliable use the objects
		// identity to identify whether the callback has already been run.
		syncSavingCallback = new Runnable() {
			@Override
			public void run() {
				// Abort if this callback has already been run manually (ex. when waiting for saving to finish):
				// Checking identity instead of null here, because if the callback has already been run manually,
				// another save might have been prepared already while this task was still pending to get run.
				if (syncSavingCallback != this) {
					return;
				}
				syncSavingCallback = null;

				// Reset save task id:
				// It is important that this gets reset inside this sync task. Otherwise another save request might get
				// prepared before this save has been fully handled.
				saveIOTask = -1;

				// Note: The save result state might still be NOT_YET_STARTED, if the saving task got cancelled before
				// it could run.

				// Mark all shopkeepers as dirty again, whose data we were not able to save:
				if (saveResult.state != SaveResult.State.SUCCESS) { // Failure or aborted or cancelled
					if (!savingShopkeepers.isEmpty()) {
						for (AbstractShopkeeper shopkeeper : savingShopkeepers) {
							shopkeeper.markDirty();
						}
						// Request another delayed save (if there isn't an abort request):
						if (!abortSave) {
							saveDelayed();
						}
					}

					// Restore number of deleted shopkeepers:
					deletedShopkeepersCount = saveResult.deletedShopkeepersCount;
				}
				savingShopkeepers.clear();

				// Remove data of shopkeepers that have been deleted during the save:
				for (AbstractShopkeeper deletedShopkeeper : shopkeepersToDelete) {
					clearShopkeeperData(deletedShopkeeper);
				}
				shopkeepersToDelete.clear();

				// If not aborted / cancelled:
				if (saveResult.state == SaveResult.State.SUCCESS || saveResult.state == SaveResult.State.FAILURE) {
					// Print debug info:
					saveResult.printDebugInfo();

					// saving failed?
					if (saveResult.state != SaveResult.State.SUCCESS) {
						// Inform admins about saving issue:
						// 4 min error message throttle (slightly less than the saving interval)
						if (Math.abs(System.currentTimeMillis() - lastSavingErrorMsgTimestamp) > (4 * 60 * 1000L)) {
							lastSavingErrorMsgTimestamp = System.currentTimeMillis();
							String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED + "Saving shop data failed! Please check out the server log(s) and look into the issue!";
							for (Player player : Bukkit.getOnlinePlayers()) {
								if (player.hasPermission(ShopkeepersPlugin.ADMIN_PERMISSION)) {
									player.sendMessage(errorMsg);
								}
							}
						}
					}
				}

				if (async) {
					// Did we get another saveReal-request in the meantime?
					if (saveAgain) {
						saveAgain = false;
						// Trigger another save with the latest data (if there isn't an abort request):
						if (!abortSave) {
							saveNow();
						}
					}
				}
			}
		};
		// Called possibly async:
		Runnable savingCallback = () -> {
			// Ensure that we continue on main thread:
			// This gets omitted here if the plugin has been disabled during an async save, in which case the callback
			// gets manually run from the onDisable handling on the main thread.
			SchedulerUtils.runOnMainThreadOrOmit(plugin, syncSavingCallback);
		};

		if (!async) {
			// Sync file IO:
			this.saveDataToFile(saveData, savingCallback);
		} else {
			// Async file IO:
			final long asyncTaskSubmittedTime = System.currentTimeMillis();
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				// Note: If the task gets cancelled, this never gets run (everything that always needs to happen, has to
				// be placed or copied into the callback as well).
				saveResult.asyncTaskDelay = System.currentTimeMillis() - asyncTaskSubmittedTime;
				// Synchronization, so that only one thread at a time attempts to mess with the save files:
				final long ioLockStartTime = System.currentTimeMillis();
				synchronized (SAVING_IO_LOCK) {
					saveResult.ioLockAcquireDuration = System.currentTimeMillis() - ioLockStartTime;
					// Did we get an abort request? -> Skip saving:
					if (abortSave) {
						saveResult.state = SaveResult.State.ABORTED;
						// If aborted, the syncSavingCallback needs to be run manually.
					} else {
						// Actual saving IO:
						this.saveDataToFile(saveData, savingCallback);
						assert saveResult.state == SaveResult.State.SUCCESS || saveResult.state == SaveResult.State.FAILURE;
					}
					// Async saving is over:
					// It is important that the save result state gets set before the lock is released, because
					// otherwise we cannot differentiate between whether the running task has already finished or is
					// still going to acquire the lock.
					assert saveResult.state != SaveResult.State.NOT_YET_STARTED;

					// Notify all possibly waiting threads:
					SAVING_IO_LOCK.notifyAll();
				}
			}).getTaskId();
		}
		currentlyProcessingSave = false;
	}

	// Max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;
	private static final Object SAVING_IO_LOCK = new Object();

	// Can be run async and sync.
	// TODO saveToString on main thread and only do the actual file writing async?
	// Because Bukkit's serialization API is not strictly thread-safe..
	private void saveDataToFile(FileConfiguration config, Runnable callback) {
		assert config != null;
		// Actual IO:
		final long ioStartTime = System.currentTimeMillis();

		File saveFile = this.getSaveFile();
		File tempSaveFile = this.getTempSaveFile();

		// Saving procedure:
		// Inside a retry-loop:
		// * If there is a temporary save file:
		// * * If there is no save file: Rename temporary save file to save file.
		// * * Else: Remove temporary save file.
		// * Create parent directories.
		// * Create new temporary save file.
		// * Save data to temporary save file.
		// * Remove old save file.
		// * Rename temporary save file to save file.

		int savingAttempt = 0;
		boolean problem = false;
		String error = null;
		Exception exception;
		boolean printStacktrace = true;

		while (++savingAttempt <= SAVING_MAX_ATTEMPTS) {
			// Reset problem variables:
			problem = false;
			error = null;
			exception = null;

			try {
				// Handle already existing temporary save file:
				if (!problem) {
					if (tempSaveFile.exists()) {
						// Check write permission:
						if (!tempSaveFile.canWrite()) {
							error = "Cannot write to temporary save file! (" + tempSaveFile.getName() + ")";
							problem = true;
						}

						if (!problem) {
							if (!saveFile.exists()) {
								// If only the temporary file exists, but the actual save file does not, this might
								// indicate, that a previous saving attempt saved to the temporary file and removed the
								// actual save file, but wasn't able to then rename the temporary file to the actual
								// save file.
								// -> The temporary file might contain the only backup of saved data, don't remove it!
								// -> Instead we try to rename it to make it the new 'actual save file' and then
								// continue the saving procedure

								Log.warning("Found an already existing temporary save file, but no old save file! (" + tempSaveFile.getName() + ")");
								Log.warning("This might indicate an issue during a previous saving attempt!");
								Log.warning("Trying to rename the temporary save file to use it as 'existing old save data', and then continue the saving!");

								// Rename temporary save file:
								if (!tempSaveFile.renameTo(saveFile)) {
									error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
									problem = true;
								}
							} else {
								// Remove old temporary save file:
								if (!tempSaveFile.delete()) {
									error = "Couldn't delete existing temporary save file! (" + tempSaveFile.getName() + ")";
									problem = true;
								}
							}
						}
					}
				}

				// Make sure that the parent directories exist:
				if (!problem) {
					File parentDir = tempSaveFile.getParentFile();
					if (parentDir != null && !parentDir.exists()) {
						if (!parentDir.mkdirs()) {
							error = "Couldn't create parent directories for temporary save file! (" + parentDir.getAbsolutePath() + ")";
							problem = true;
						}
					}
				}

				// Create new temporary save file:
				if (!problem) {
					try {
						tempSaveFile.createNewFile();
					} catch (Exception e) {
						error = "Couldn't create temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
						exception = e;
						problem = true;
					}
				}

				// Write shopkeeper data to temporary save file:
				if (!problem) {
					PrintWriter writer = null;
					try {
						String fileEncoding = Settings.async().fileEncoding;
						if (fileEncoding != null && !fileEncoding.isEmpty()) {
							writer = new PrintWriter(tempSaveFile, fileEncoding);
							writer.write(config.saveToString());
						} else {
							config.save(tempSaveFile);
						}
					} catch (Exception e) {
						error = "Couldn't save data to temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
						exception = e;
						problem = true;
					} finally {
						if (writer != null) {
							writer.close();
						}
					}
				}

				// Delete old save file:
				if (!problem) {
					if (saveFile.exists()) {
						// Check write permission:
						if (!saveFile.canWrite()) {
							error = "Cannot write to save file! (" + saveFile.getName() + ")";
							problem = true;
						} else {
							// Delete old save file:
							if (!saveFile.delete()) {
								error = "Couldn't delete existing old save file! (" + saveFile.getName() + ")";
								problem = true;
							}
						}
					}
				}

				// Rename temporary save file:
				if (!problem) {
					if (!tempSaveFile.renameTo(saveFile)) {
						error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
						problem = true;
					}
				}
			} catch (Exception e) {
				// Catching any exceptions not explicitly caught above already:
				error = e.getMessage();
				exception = e;
				problem = true;
			}

			// Handle problem situation:
			if (problem) {
				// Don't spam with errors and stacktraces, only print them once for the first saving attempt:
				if (exception != null && printStacktrace) {
					printStacktrace = false;
					exception.printStackTrace();
				}
				Log.severe("Saving attempt " + savingAttempt + " failed: " + (error != null ? error : "Unknown error"));

				if (savingAttempt < SAVING_MAX_ATTEMPTS) {
					// Try again after a small delay:
					try {
						Thread.sleep(SAVING_ATTEMPTS_DELAY_MILLIS);
					} catch (InterruptedException e) {
					}
				} else {
					// Saving failed even after a bunch of retries:
					saveResult.state = SaveResult.State.FAILURE;
					Log.severe("Saving failed! Save data might be lost! :(");
					break;
				}
			} else {
				// Saving was successful:
				saveResult.state = SaveResult.State.SUCCESS;
				break;
			}
		}

		final long now = System.currentTimeMillis();
		saveResult.ioDuration = now - ioStartTime; // Time for pure io
		saveResult.totalDuration = now - saveResult.startTime; // Time from saveReal() call to finished save

		// File IO is over.

		// Run callback:
		if (callback != null) {
			callback.run();
		}
	}

	private static class SaveResult {

		// Note: Synchronization for those values is not needed, because they get synchronized externally before getting
		// used, either by the bukkit scheduler (when starting the async task and when going back to the main thread by
		// starting a sync task), or via synchronization of the SAVING_IO_LOCK.

		public enum State {
			NOT_YET_STARTED,
			SUCCESS,
			FAILURE,
			ABORTED;
		}

		private State state;
		private int dirtyShopkeepersCount = 0;
		private int deletedShopkeepersCount = 0;
		private boolean async;
		private long startTime;
		private long packingDuration;
		private long asyncTaskDelay;
		private long ioLockAcquireDuration;
		private long ioDuration;
		private long totalDuration;

		public void reset() {
			state = State.NOT_YET_STARTED;
			// All other attributes get set appropriately before getting used.
		}

		public void printDebugInfo() {
			Log.debug(() -> "Saved shopkeeper data (" + totalDuration + "ms (Data packing ("
					+ dirtyShopkeepersCount + " dirty, " + deletedShopkeepersCount + " deleted): " + packingDuration + "ms, "
					+ (async ? "AsyncTask delay: " + asyncTaskDelay + "ms, " : "")
					+ ((ioLockAcquireDuration > 1) ? "IO lock delay: " + ioLockAcquireDuration + "ms, " : "")
					+ (async ? "Async " : "Sync ") + "IO: " + ioDuration + "ms))"
					+ ((state == State.FAILURE) ? ": Saving failed!" : ""));
		}
	}
}
