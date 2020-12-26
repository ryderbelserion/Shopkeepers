package com.nisovin.shopkeepers.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SingletonTask;
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
 * <li>It is not safe to externally edit the save file while the plugin is running, since it might overwrite the save
 * file at any time.
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

	// Max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;

	private final SKShopkeepersPlugin plugin;
	private final int minecraftDataVersion;
	private final DataVersion currentDataVersion;

	/* Data */
	/*
	 * Holds the data that gets used by the current/next (possibly async) save.
	 * This also contains any data of shopkeepers that could not be loaded correctly.
	 * This cannot be modified while an async save is in progress.
	 */
	private final FileConfiguration saveData = new YamlConfiguration();
	private int maxStoredShopkeeperId = 0;
	private int nextShopkeeperId = 1;

	/* Loading */
	private boolean currentlyLoading = false;

	/* Saving */
	private final SaveTask saveTask;
	// Flag to (temporarily) turn off saving:
	private boolean savingDisabled = false;
	// There might be shopkeepers with unsaved data, or we got an explicit save request:
	private boolean dirty = false;
	private BukkitTask delayedSaveTask = null;
	// Shopkeepers that got deleted during the last async save:
	private final List<AbstractShopkeeper> shopkeepersToDelete = new ArrayList<>();
	// Number of shopkeepers whose data got removed since the last save:
	private int deletedShopkeepersCount = 0;

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.minecraftDataVersion = this.getMinecraftDataVersion();
		this.currentDataVersion = new DataVersion(SHOPKEEPERS_DATA_VERSION, minecraftDataVersion);
		this.saveTask = new SaveTask(plugin);
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
		// Start periodic save task:
		if (!Settings.saveInstantly) {
			this.startPeriodicSaveTask();
		}
	}

	public void onDisable() {
		// Ensure that there is no unsaved data and that all saves are completed before we continue:
		this.saveIfDirtyAndAwaitSaves();

		// Reset a few things:
		saveTask.onDisable();
		this.clearSaveData();
		savingDisabled = false;
		dirty = false;
		delayedSaveTask = null;
		shopkeepersToDelete.clear();
		deletedShopkeepersCount = 0;
	}

	private void startPeriodicSaveTask() {
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

	// Also takes ids of stored shopkeepers in account that could not be loaded for some reason.
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
		if (saveTask.isRunning()) {
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
	// This is blocking and will wait for any currently on-going or pending saves to complete!
	public boolean reload() {
		if (currentlyLoading) {
			throw new IllegalStateException("Already loading right now!");
		}

		// To avoid concurrent access of the save file, we wait for any ongoing and pending saves to complete:
		// TODO Skip the reload if we just triggered another save? The reloaded data is expected to match the data we
		// just saved.
		this.saveIfDirtyAndAwaitSaves();

		currentlyLoading = true;
		boolean result;
		try {
			result = this.doReload();
		} catch (Exception e) {
			Log.severe("Something completely unexpected went wrong during the loading of the saved shopkeepers data!", e);
			result = false; // Error
		} finally {
			currentlyLoading = false;
		}
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
			// Load with the specified encoding:
			// Note: The config's load method wraps the reader into a BufferedReader.
			try (	FileInputStream stream = new FileInputStream(saveFile);
					InputStreamReader reader = new InputStreamReader(stream, DerivedSettings.fileCharset)) {
				saveData.load(reader);
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

	// SAVING

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
		if (Settings.saveInstantly && delayedSaveTask == null) {
			delayedSaveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
				delayedSaveTask = null;
				if (this.isDirty()) {
					this.saveNow();
				}
			}, 600); // 30 seconds delay
		} // Else: The periodic save task will trigger a save at some point.
	}

	@Override
	public void saveNow() {
		this.doSave(true);
	}

	@Override
	public void saveImmediate() {
		this.doSave(false);
	}

	/**
	 * Requests a save if there is unsaved data.
	 * <p>
	 * This also waits for any current and pending saves to complete.
	 */
	public void saveIfDirtyAndAwaitSaves() {
		if (this.isDirty()) {
			this.saveImmediate();
		} else {
			saveTask.awaitExecutions();
		}
	}

	private void doSave(boolean async) {
		if (savingDisabled) {
			Log.warning("Skipping save, because saving got disabled.");
			return;
		}

		if (async) {
			saveTask.run();
		} else {
			saveTask.runImmediately();
		}
	}

	private class SaveTask extends SingletonTask {

		// Previously dirty shopkeepers which we currently attempt to save:
		private final List<AbstractShopkeeper> savingShopkeepers = new ArrayList<>();

		/* Last save */
		// These variables get replaced during the next save.
		// Note: Explicit synchronization is not needed for these variables, because they already get synchronized
		// before they are used, either by the Bukkit Scheduler (when starting the async task and when going back to the
		// main thread by starting a sync task), or/and via synchronization with the save task's lock.
		private boolean savingFailed = false;
		private long lastSavingErrorMsgTimestamp = 0L;
		private int lastSaveDirtyShopkeepersCount = 0;
		private int lastSaveDeletedShopkeepersCount = 0;

		SaveTask(Plugin plugin) {
			super(plugin);
		}

		void onDisable() {
			lastSavingErrorMsgTimestamp = 0L;
		}

		@Override
		protected void prepare() {
			// Stop current delayed save task:
			if (delayedSaveTask != null) {
				delayedSaveTask.cancel();
				delayedSaveTask = null;
			}

			// Setup the file header:
			// This replaces any previously existing and loaded header and thereby ensures that it is always up-to-date
			// after we have saved the file.
			saveData.options().header(HEADER);

			// Store data of dirty shopkeepers into memory configuration:
			lastSaveDirtyShopkeepersCount = 0;
			for (AbstractShopkeeper shopkeeper : getShopkeeperRegistry().getAllShopkeepers()) {
				if (!shopkeeper.isDirty()) {
					continue; // Assume storage data is still up-to-date
				}
				lastSaveDirtyShopkeepersCount++;

				String sectionKey = String.valueOf(shopkeeper.getId());
				Object previousData = saveData.get(sectionKey);
				ConfigurationSection newSection = saveData.createSection(sectionKey); // Replaces the previous section
				try {
					shopkeeper.save(newSection);
				} catch (Exception e) {
					// Error while saving shopkeeper data:
					// Restore previous shopkeeper data and then skip this shopkeeper.
					saveData.set(sectionKey, previousData);
					Log.warning("Could not save shopkeeper '" + shopkeeper.getId() + "'!", e);
					// The shopkeeper stays marked as dirty, so we attempt to save it again the next time we save all
					// shops.
					// However, we won't automatically initiate a new save for this shopkeeper as the risk is high that
					// saving might fail again anyways.
					continue;
				}

				savingShopkeepers.add(shopkeeper);
				shopkeeper.onSave();
			}

			// Store number of deleted shopkeepers (for debugging purposes):
			lastSaveDeletedShopkeepersCount = deletedShopkeepersCount;
			deletedShopkeepersCount = 0;

			// Note: The dirty flag might get reverted again after saving, if saving failed.
			// However, the flag gets reset here (and not just after successful saving), so that any saving requests
			// that arrive in the meantime get noticed and can cause another save later:
			dirty = false;
		}

		@Override
		protected void execute() {
			this.saveToFile(saveData);
		}

		// Can be run async and sync.
		private void saveToFile(FileConfiguration config) {
			assert config != null;
			File saveFile = getSaveFile();
			File tempSaveFile = getTempSaveFile();

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
									// indicate, that a previous saving attempt saved to the temporary file and removed
									// the actual save file, but wasn't able to then rename the temporary file to the
									// actual save file.
									// -> The temporary file might contain the only backup of saved data, don't remove
									// it!
									// -> Instead we try to rename it to make it the new 'actual save file' and then
									// continue the saving procedure

									Log.warning("Found an already existing temporary save file, but no old save file! (" + tempSaveFile.getName() + ")");
									Log.warning("This might indicate an issue during a previous saving attempt!");
									Log.warning("Trying to rename the temporary save file to use it as 'existing old save data', and then continue the saving!");

									// Rename temporary save file:
									if (!tempSaveFile.renameTo(saveFile)) {
										error = "Could not rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
										problem = true;
									}
								} else {
									// Remove old temporary save file:
									if (!tempSaveFile.delete()) {
										error = "Could not delete existing temporary save file! (" + tempSaveFile.getName() + ")";
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
								error = "Could not create parent directories for temporary save file! (" + parentDir.getAbsolutePath() + ")";
								problem = true;
							}
						}
					}

					// Create new temporary save file:
					if (!problem) {
						try {
							tempSaveFile.createNewFile();
						} catch (Exception e) {
							error = "Could not create temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
							exception = e;
							problem = true;
						}
					}

					// Serialize data to String:
					// TODO Do this on the main thread? Bukkit's serialization API is not strictly thread-safe..
					// However, this should usually not be an issue if the serialized objects inside the config are not
					// accessed externally, and do not rely on external state during serialization.
					String data = null;
					if (!problem) {
						try {
							data = config.saveToString();
						} catch (Exception e) {
							error = "Could not serialize shopkeeper data: " + e.getMessage();
							exception = e;
							problem = true;
						}
					}

					// Write shopkeeper data to temporary save file:
					if (!problem) {
						assert data != null;
						// Save with the specified encoding:
						// Note: BufferedWriter is not required here, since we invoke the writer only once.
						try (	FileOutputStream stream = new FileOutputStream(tempSaveFile);
								OutputStreamWriter writer = new OutputStreamWriter(stream, DerivedSettings.fileCharset)) {
							writer.write(data);
						} catch (Exception e) {
							error = "Could not save data to temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
							exception = e;
							problem = true;
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
									error = "Could not delete existing old save file! (" + saveFile.getName() + ")";
									problem = true;
								}
							}
						}
					}

					// Rename temporary save file:
					if (!problem) {
						if (!tempSaveFile.renameTo(saveFile)) {
							error = "Could not rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
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
						// Saving failed even after several retries:
						savingFailed = true;
						Log.severe("Saving failed! Data might have been lost! :(");
						break;
					}
				} else {
					// Saving was successful:
					savingFailed = false;
					break;
				}
			}
		}

		@Override
		protected void syncCallback() {
			// Print debug info:
			printDebugInfo();

			// Saving failed?
			if (savingFailed) {
				// Mark all shopkeepers as dirty again whose data we were not able to save:
				if (!savingShopkeepers.isEmpty()) {
					for (AbstractShopkeeper shopkeeper : savingShopkeepers) {
						shopkeeper.markDirty();
					}

					// Request another delayed save:
					saveDelayed();
				}

				// Restore number of deleted shopkeepers:
				// Note: Further shopkeepers might have been deleted in the meantime, so we add the numbers.
				deletedShopkeepersCount += lastSaveDeletedShopkeepersCount;

				// Inform admins about saving issue:
				// Error message throttle of 4 minutes (slightly less than the saving interval)
				if (Math.abs(System.currentTimeMillis() - lastSavingErrorMsgTimestamp) > (4 * 60 * 1000L)) {
					lastSavingErrorMsgTimestamp = System.currentTimeMillis();
					String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED + "Saving shopkeepers failed! Please check the server logs and look into the issue!";
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player.hasPermission(ShopkeepersPlugin.ADMIN_PERMISSION)) {
							player.sendMessage(errorMsg);
						}
					}
				}
			}
			savingShopkeepers.clear();

			// Remove data of shopkeepers that have been deleted in the meantime:
			for (AbstractShopkeeper deletedShopkeeper : shopkeepersToDelete) {
				clearShopkeeperData(deletedShopkeeper);
			}
			shopkeepersToDelete.clear();
		}

		private void printDebugInfo() {
			Log.debug(() -> "Saved shopkeeper data (" + lastSaveDirtyShopkeepersCount + " dirty, "
					+ lastSaveDeletedShopkeepersCount + " deleted): " + this.getExecutionTimingString()
					+ (savingFailed ? " -- Saving failed!" : ""));
		}
	}
}
