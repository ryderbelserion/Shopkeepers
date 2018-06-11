package com.nisovin.shopkeepers.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shoptypes.AbstractShopType;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Implementation notes:<br>
 * <ul>
 * <li>There can at most be one thread doing file IO at the same time.
 * <li>Saving preparation always happens on the server's main thread. There can at most be one save getting prepared at
 * the same time.
 * <li>If there is a request for an <b>async</b> save while an async save is already in progress, a flag gets set to
 * indicate this after the current async save is finished.
 * <li>If there is a request for a <b>sync</b> save while an async save is already in progress, the main thread waits
 * for the async save to finish, before preparing the next save.
 * </ul>
 */
public class SKShopkeeperStorage implements ShopkeeperStorage {

	private final SKShopkeepersPlugin plugin;

	// data:
	private final FileConfiguration saveData = new YamlConfiguration();
	private int maxStoredShopkeeperId = 0;
	private int nextShopkeeperId = 1;

	// flag to (temporary) turn off saving
	private boolean savingDisabled = false;
	private long lastSavingErrorMsgTimestamp = 0L;
	// there might be shopkeepers with unsaved data, or we got an explicit save request:
	private boolean dirty = false; //
	private int delayedSaveTaskId = -1;

	// current loading:
	private boolean currentlyLoading = false;

	// current saving:
	// indicates if we are currently processing a save (gets reset to false once the save has been processed on the main
	// thread, or handed over to the async IO task):
	private boolean currentlyProcessingSave = false;
	private final SaveResult saveResult = new SaveResult();
	// previously dirty shopkeepers which we currently attempt to save:
	private final List<AbstractShopkeeper> savingShopkeepers = new ArrayList<>();
	// buffer that holds the data that gets used by the current async save task:
	// needs to be kept in sync with the save data, but cannot be modified during the async save is in progress
	private final FileConfiguration saveDataBuffer = new YamlConfiguration();
	// the task which performs async file io during a save:
	private int saveIOTask = -1;
	// the saving callback of the current save: may need to be run manually during plugin isable
	private Runnable syncSavingCallback = null;
	// determines if there was another saveReal-request while the saveIOTask was still in progress
	private boolean saveAgain = false;
	// shopkeepers that got deleted during the last async save:
	private final List<AbstractShopkeeper> deletedShopkeepers = new ArrayList<>();

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// start save task:
		if (!Settings.saveInstantly) {
			Bukkit.getScheduler().runTaskTimer(plugin, () -> {
				if (this.isDirty()) {
					this.saveNow();
				}
			}, 6000, 6000); // 5 minutes
		}
	}

	public void onDisable() {
		// wait for any async saving to finish:
		this.waitForAsyncSave();

		// save if dirty:
		if (this.isDirty()) {
			this.saveImmediate(); // not async here
		}

		// reset a few things:
		this.clearSaveData();
		savingShopkeepers.clear();
		savingDisabled = false;
		lastSavingErrorMsgTimestamp = 0L;
		dirty = false;
		delayedSaveTaskId = -1;
		saveIOTask = -1;
		saveAgain = false;
	}

	private SKShopkeeperRegistry getShopkeeperRegistry() {
		return plugin.getShopkeeperRegistry();
	}

	public void disableSaving() {
		this.savingDisabled = true;
	}

	public void enableSaving() {
		this.savingDisabled = false;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
	}

	private File getSaveFile() {
		return new File(plugin.getDataFolder(), "save.yml");
	}

	private File getTempSaveFile() {
		File saveFile = this.getSaveFile();
		return new File(saveFile.getParentFile(), saveFile.getName() + ".temp");
	}

	// SHOPKEEPER IDs

	// does not increment the shopkeeper id counter on its own (we don't want to increment it in case the shopkeeper
	// creation fails)
	public int getNextShopkeeperId() {
		int nextId = nextShopkeeperId; // can end up negative after increments due to overflows
		if (nextId <= 0 || !this.isUnusedId(nextId)) {
			// try to use an id larger than the max currently used id:
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
				// find the first unused id:
				nextId = 1;
				while (!this.isUnusedId(nextId)) {
					nextId++;
					if (nextId <= 0) {
						// overflow, all ids are in use..:
						throw new IllegalStateException("No unused shopkeeper ids available!");
					}
				}
				assert nextId > 0;
			}
			// remember the found next id:
			nextShopkeeperId = nextId;
		}
		return nextId;
	}

	// also takes ids of stored shopkeepers in account that couldn't be loaded for some reason
	private boolean isUnusedId(int id) {
		return (!saveData.contains(String.valueOf(id)) && this.getShopkeeperRegistry().getShopkeeperById(id) == null);
	}

	public void onShopkeeperIdUsed(int id) {
		if (id >= nextShopkeeperId) nextShopkeeperId = id + 1;
	}

	// SHOPKEEPER DATA REMOVAL

	private void clearSaveData() {
		this.clearConfigSection(saveData);
		this.clearConfigSection(saveDataBuffer);
		maxStoredShopkeeperId = 0;
		nextShopkeeperId = 1;
	}

	private void clearConfigSection(ConfigurationSection configSection) {
		assert configSection != null;
		for (String key : configSection.getKeys(false)) {
			configSection.set(key, null);
		}
	}

	public void clearShopkeeperData(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (this.isCurrentlySavingAsync()) {
			// remember to remove the data after the current async save has finished:
			deletedShopkeepers.add(shopkeeper);
		} else {
			String key = String.valueOf(shopkeeper.getId());
			saveData.set(key, null);
			saveDataBuffer.set(key, null);
		}
	}

	// LOADING

	// returns true on success, and false if there was some severe issue during loading
	public boolean reload() {
		if (currentlyLoading) {
			throw new IllegalStateException("Already loading right now!");
		}
		currentlyLoading = true;
		boolean result;

		// no concurrent access of the save file:
		synchronized (SAVING_IO_LOCK) {
			try {
				result = this.doReload();
			} catch (Exception e) {
				Log.severe("Something completely unexpected went wrong during the loading of the saved shopkeepers data!", e);
				result = false; // error
			}
		}
		currentlyLoading = false;
		return result;
	}

	// returns true on success, and false if there was some severe issue during loading
	private boolean doReload() {
		// unload all currently loaded shopkeepers:
		SKShopkeeperRegistry shopkeeperRegistry = this.getShopkeeperRegistry();
		shopkeeperRegistry.unloadAllShopkeepers();
		this.clearSaveData();

		File saveFile = this.getSaveFile();
		if (!saveFile.exists()) {
			File tempSaveFile = this.getTempSaveFile();
			if (tempSaveFile.exists()) {
				// load from temporary save file instead:
				Log.warning("Found no save file, but an existing temporary save file! (" + tempSaveFile.getName() + ")");
				Log.warning("This might indicate an issue during a previous saving attempt!");
				Log.warning("Trying to load the shopkeepers data from this temporary save file instead!");

				saveFile = tempSaveFile;
			} else {
				// save file does not exist yet -> no shopkeeper data available
				return true;
			}
		}

		try {
			if (!StringUtils.isEmpty(Settings.fileEncoding)) {
				// load with specified charset:
				try (	FileInputStream stream = new FileInputStream(saveFile);
						InputStreamReader reader = new InputStreamReader(stream, Settings.fileEncoding)) {
					saveData.load(reader);
				}
			} else {
				// load with default charset handling:
				saveData.load(saveFile);
			}
		} catch (Exception e) {
			Log.severe("Failed to load save file!", e);
			return false; // disable without save
		}

		Set<String> keys = saveData.getKeys(false);
		Log.info("Loading data of " + keys.size() + " shopkeepers..");
		for (String key : keys) {
			Integer idInt = Utils.parseInt(key);
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
				continue; // skip this shopkeeper
			}

			String shopTypeString = shopkeeperSection.getString("type");
			AbstractShopType<?> shopType = plugin.getShopTypeRegistry().get(shopTypeString);
			// unknown shop type
			if (shopType == null) {
				Log.warning("Failed to load shopkeeper '" + key + "': Unknown shop type: " + shopTypeString);
				continue; // skip this shopkeeper
			}

			// performs additional data validation and migrations:
			boolean preparationResult = this.prepareShopkeeperData(id, shopkeeperSection);
			if (!preparationResult) {
				continue; // skip this shopkeeper
			}

			// load shopkeeper:
			try {
				shopkeeperRegistry.loadShopkeeper(shopType, id, shopkeeperSection);
			} catch (ShopkeeperCreateException e) {
				Log.warning("Failed to load shopkeeper '" + key + "': " + e.getMessage());
				continue; // skip this shopkeeper
			} catch (Exception e) {
				Log.warning("Failed to load shopkeeper '" + key + "'", e);
				continue; // skip this shopkeeper
			}
		}

		// create a copy of the save data's top level data structure:
		this.clearConfigSection(saveDataBuffer);
		for (Entry<String, Object> entry : saveData.getValues(false).entrySet()) {
			saveDataBuffer.set(entry.getKey(), entry.getValue());
		}

		return true;
	}

	// validates and performs migration of the save data
	// returns false to skip this shopkeeper
	private boolean prepareShopkeeperData(int id, ConfigurationSection shopkeeperSection) {
		// MC 1.11: convert old skeleton and zombie variants to new mob types
		// TODO remove again in future updates
		boolean hasStrayType = false;
		boolean hasWitherSkeletonType = false;
		boolean hasZombieVillagerType = false;
		try {
			hasStrayType = (EntityType.valueOf("STRAY") != null);
		} catch (Exception e) {
		}
		try {
			hasWitherSkeletonType = (EntityType.valueOf("WITHER_SKELETON") != null);
		} catch (Exception e) {
		}
		try {
			hasZombieVillagerType = (EntityType.valueOf("ZOMBIE_VILLAGER") != null);
		} catch (Exception e) {
		}

		if (hasStrayType || hasWitherSkeletonType || hasZombieVillagerType) {
			String objectType = shopkeeperSection.getString("object");
			if ("skeleton".equalsIgnoreCase(objectType)) {
				String skeletonType = shopkeeperSection.getString("skeletonType");
				if (hasStrayType && "STRAY".equalsIgnoreCase(skeletonType)) {
					Log.warning("Converting skeleton shopkeeper '" + id + "' with stray variant to new stray shopkeeper.");
					shopkeeperSection.set("object", "stray");
				} else if (hasWitherSkeletonType && "WITHER".equalsIgnoreCase(skeletonType)) {
					Log.warning("Converting skeleton shopkeeper '" + id + "' with wither variant to new wither-skeleton shopkeeper.");
					shopkeeperSection.set("object", "wither_skeleton");
				}
			}
			if ("zombie".equalsIgnoreCase(objectType)) {
				if (hasZombieVillagerType && shopkeeperSection.getBoolean("villagerZombie")) {
					Log.warning("Converting zombie shopkeeper '" + id + "' with zombie-villager variant to new zombie-villager shopkeeper.");
					shopkeeperSection.set("object", "zombie_villager");
				}
			}
		}
		return true;
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

	private boolean isCurrentlySavingAsync() {
		return (saveIOTask != -1);
	}

	// gets run from main thread
	private void waitForAsyncSave() {
		assert Bukkit.isPrimaryThread();
		// wait for async save to finish:
		synchronized (SAVING_IO_LOCK) {
			// manually run the callback of the previous save here, to complete the previous save before continuing:
			if (syncSavingCallback != null) {
				syncSavingCallback.run();
			}
		}
	}

	private void saveReal(boolean async) {
		if (savingDisabled) {
			Log.warning("Skipping saving because it got disabled.");
			return;
		}

		// stop current delayed save task:
		if (delayedSaveTaskId != -1) {
			Bukkit.getScheduler().cancelTask(delayedSaveTaskId);
			delayedSaveTaskId = -1;
		}

		if (currentlyProcessingSave) {
			Log.warning("Ignoring saving reqest: We are already processing a save right now!");
			return;
		}
		currentlyProcessingSave = true;

		// is another async save task already running?
		if (this.isCurrentlySavingAsync()) {
			if (async) {
				// set flag which triggers a new save once that current task is done:
				saveAgain = true;
				currentlyProcessingSave = false;
				return;
			} else {
				// wait for async save to finish:
				this.waitForAsyncSave();
			}
		}

		// keeps track of statistics and information about this saving attempt:
		saveResult.async = async;
		saveResult.startTime = System.currentTimeMillis();

		// store data of dirty shopkeepers into memory configuration:
		saveResult.dirtyShopkeeperCount = 0;
		for (AbstractShopkeeper shopkeeper : this.getShopkeeperRegistry().getAllShopkeepers()) {
			if (!shopkeeper.isDirty()) {
				continue; // assume storage data is still up-to-date
			}
			saveResult.dirtyShopkeeperCount++;

			String sectionKey = String.valueOf(shopkeeper.getId());
			Object previousData = saveData.get(sectionKey);
			ConfigurationSection newSection = saveData.createSection(sectionKey);
			try {
				shopkeeper.save(newSection);
			} catch (Exception e) {
				// error while saving shopkeeper data:
				// restore previous shopkeeper data and then skip this shopkeeper:
				saveData.set(sectionKey, previousData);
				Log.warning("Couldn't save shopkeeper '" + shopkeeper.getId() + "'!", e);
				// the shopkeeper stays marked as dirty, so we attempt to save it again the next time we save all shops
				// however, we won't automatically initiate a new save for this shopkeeper as the risk is high that
				// saving might fail again anyways
				continue;
			}
			// update save data buffer:
			saveDataBuffer.set(sectionKey, newSection);

			savingShopkeepers.add(shopkeeper);
			shopkeeper.onSave();
		}

		// time to store shopkeeper data in memory configuration:
		saveResult.packingDuration = System.currentTimeMillis() - saveResult.startTime;

		// note: the dirty flag might get reverted again after saving, if saving failed
		// however, the flag gets reset here (and not just after successful saving), so that any saving requests that
		// arrive in the meantime get noticed and can cause another save later:
		dirty = false;

		// called sync:
		syncSavingCallback = () -> {
			if (syncSavingCallback == null) {
				// the callback has already been run manually:
				return;
			}
			syncSavingCallback = null;

			// print debug info:
			saveResult.printDebugInfo();

			// saving failed?
			if (!saveResult.success) {
				// mark all shopkeepers as dirty again, whose data we were not able to save, and request another delayed
				// save (if there isn't a save waiting already):
				for (AbstractShopkeeper shopkeeper : savingShopkeepers) {
					shopkeeper.markDirty();
				}
				if (!currentlyProcessingSave) {
					this.saveDelayed();
				}

				// inform admins about saving issue:
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
			savingShopkeepers.clear();

			if (async) {
				// async save is over:
				saveIOTask = -1;

				// shopkeepers were removed during the async save:
				for (AbstractShopkeeper deletedShopkeeper : deletedShopkeepers) {
					this.clearShopkeeperData(deletedShopkeeper);
				}
				deletedShopkeepers.clear();

				// did we get another saveReal-request in the meantime?
				if (saveAgain) {
					// trigger another full save with latest data (if there isn't a save waiting already):
					saveAgain = false;
					if (!currentlyProcessingSave) {
						this.saveNow();
					}
				}
			}
		};
		// called possibly async:
		Runnable savingCallback = () -> {
			// ensure that we continue on main thread:
			// this gets omitted here if the plugin has been disabled during an async save,
			// in which case the callback gets manually run from the onDisable handling of the main thread
			SchedulerUtils.runOnMainThreadOrOmit(plugin, syncSavingCallback);
		};

		if (!async) {
			// sync file io:
			this.saveDataToFile(saveDataBuffer, savingCallback);
		} else {
			// async file io:
			final long asyncTaskSubmittedTime = System.currentTimeMillis();
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				saveResult.asyncTaskDelay = System.currentTimeMillis() - asyncTaskSubmittedTime;
				this.saveDataToFile(saveDataBuffer, savingCallback);
			}).getTaskId();
		}
		currentlyProcessingSave = false;
	}

	// max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;
	private static final Object SAVING_IO_LOCK = new Object();

	// can be run async and sync
	private void saveDataToFile(FileConfiguration config, Runnable callback) {
		assert config != null;

		// synchronization, so that only one thread at a time attempts to mess with the save files
		final long ioLockStartTime = System.currentTimeMillis();
		synchronized (SAVING_IO_LOCK) {
			saveResult.ioLockAcquireDuration = System.currentTimeMillis() - ioLockStartTime;

			// actual IO:
			final long ioStartTime = System.currentTimeMillis();

			File saveFile = this.getSaveFile();
			File tempSaveFile = this.getTempSaveFile();

			// saving procedure:
			// inside a retry-loop:
			// * if there is a temporary save file:
			// * * if there is no save file: rename temporary save file to save file
			// * * else: remove temporary save file
			// * create parent directories
			// * create new temporary save file
			// * save data to temporary save file
			// * remove old save file
			// * rename temporary save file to save file

			int savingAttempt = 0;
			boolean problem = false;
			String error = null;
			Exception exception;
			boolean printStacktrace = true;

			while (++savingAttempt <= SAVING_MAX_ATTEMPTS) {
				// reset problem variables:
				problem = false;
				error = null;
				exception = null;

				try {
					// handle already existing temporary save file:
					if (!problem) {
						if (tempSaveFile.exists()) {
							// check write permission:
							if (!tempSaveFile.canWrite()) {
								error = "Cannot write to temporary save file! (" + tempSaveFile.getName() + ")";
								problem = true;
							}

							if (!problem) {
								if (!saveFile.exists()) {
									// if only the temporary file exists, but the actual save file does not, this might
									// indicate, that a previous saving attempt saved to the temporary file and removed
									// the
									// actual save file, but wasn't able to then rename the temporary file to the actual
									// save file
									// -> the temporary file might contain the only backup of saved data, don't remove
									// it!
									// -> instead we try to rename it to make it the new 'actual save file' and then
									// continue the saving procedure

									Log.warning("Found an already existing temporary save file, but no old save file! (" + tempSaveFile.getName() + ")");
									Log.warning("This might indicate an issue during a previous saving attempt!");
									Log.warning("Trying to rename the temporary save file to use it as 'existing old save data', and then continue the saving!");

									// rename temporary save file:
									if (!tempSaveFile.renameTo(saveFile)) {
										error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
										problem = true;
									}
								} else {
									// remove old temporary save file:
									if (!tempSaveFile.delete()) {
										error = "Couldn't delete existing temporary save file! (" + tempSaveFile.getName() + ")";
										problem = true;
									}
								}
							}
						}
					}

					// make sure that the parent directories exist:
					if (!problem) {
						File parentDir = tempSaveFile.getParentFile();
						if (parentDir != null && !parentDir.exists()) {
							if (!parentDir.mkdirs()) {
								error = "Couldn't create parent directories for temporary save file! (" + parentDir.getAbsolutePath() + ")";
								problem = true;
							}
						}
					}

					// create new temporary save file:
					if (!problem) {
						try {
							tempSaveFile.createNewFile();
						} catch (Exception e) {
							error = "Couldn't create temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
							exception = e;
							problem = true;
						}
					}

					// write shopkeeper data to temporary save file:
					if (!problem) {
						PrintWriter writer = null;
						try {
							if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
								writer = new PrintWriter(tempSaveFile, Settings.fileEncoding);
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

					// delete old save file:
					if (!problem) {
						if (saveFile.exists()) {
							// check write permission:
							if (!saveFile.canWrite()) {
								error = "Cannot write to save file! (" + saveFile.getName() + ")";
								problem = true;
							} else {
								// delete old save file:
								if (!saveFile.delete()) {
									error = "Couldn't delete existing old save file! (" + saveFile.getName() + ")";
									problem = true;
								}
							}
						}
					}

					// rename temporary save file:
					if (!problem) {
						if (!tempSaveFile.renameTo(saveFile)) {
							error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
							problem = true;
						}
					}
				} catch (Exception e) {
					// catching any exceptions not explicitly caught above already:
					error = e.getMessage();
					exception = e;
					problem = true;
				}

				// handle problem situation:
				if (problem) {
					// don't spam with errors and stacktraces, only print them once for the first saving attempt:
					if (exception != null && printStacktrace) {
						printStacktrace = false;
						exception.printStackTrace();
					}
					Log.severe("Saving attempt " + savingAttempt + " failed: " + (error != null ? error : "Unknown error"));

					if (savingAttempt < SAVING_MAX_ATTEMPTS) {
						// try again after a small delay:
						try {
							Thread.sleep(SAVING_ATTEMPTS_DELAY_MILLIS);
						} catch (InterruptedException e) {
						}
					} else {
						// saving failed even after a bunch of retries:
						saveResult.success = false;
						Log.severe("Saving failed! Save data might be lost! :(");
						break;
					}
				} else {
					// saving was successful:
					saveResult.success = true;
					break;
				}
			}

			final long now = System.currentTimeMillis();
			saveResult.ioDuration = now - ioStartTime; // time for pure io
			saveResult.totalDuration = now - saveResult.startTime; // time from saveReal() call to finished save

			// file IO over

			// run callback:
			if (callback != null) {
				callback.run();
			}
		}
	}

	private static class SaveResult {

		// note: synchronization for those values is not needed, because they don't get modified by the main and async
		// task at the same time, and because the bukkit scheduler already includes memory-barriers when starting the
		// async IO task and when going back to the main thread by starting a sync task, so changes become visible to
		// each thread involved

		private int dirtyShopkeeperCount = 0;
		private boolean async;
		private long startTime;
		private long packingDuration;
		private long asyncTaskDelay;
		private long ioLockAcquireDuration;
		private long ioDuration;
		private long totalDuration;
		private boolean success;

		public void printDebugInfo() {
			Log.debug("Saved shopkeeper data (" + totalDuration + "ms (Data packing ("
					+ dirtyShopkeeperCount + " dirty shopkeepers): " + packingDuration + "ms, "
					+ (async ? "AsyncTask delay: " + asyncTaskDelay + "ms, " : "")
					+ ((ioLockAcquireDuration > 1) ? "IO lock delay: " + ioLockAcquireDuration + "ms, " : "")
					+ (async ? "Async " : "Sync ") + "IO: " + ioDuration + "ms))"
					+ (success ? "" : ": Saving failed!"));
		}
	}
}
