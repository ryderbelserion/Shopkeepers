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
import org.bukkit.scheduler.BukkitWorker;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.StringUtils;

/**
 * Implementation notes:<br>
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

	private final SKShopkeepersPlugin plugin;

	// data:
	private final FileConfiguration saveData = new YamlConfiguration();
	private int maxStoredShopkeeperId = 0;
	private int nextShopkeeperId = 1;

	// flag to (temporary) turn off saving
	private boolean savingDisabled = false;
	private long lastSavingErrorMsgTimestamp = 0L;
	// there might be shopkeepers with unsaved data, or we got an explicit save request:
	private boolean dirty = false;
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
	// the saving callback of the current save: may need to be run manually during plugin disable or save abortion
	private Runnable syncSavingCallback = null;
	// whether there was an abort request for the last async save:
	private boolean abortSave = false;
	// determines if there was another saveReal-request while the saveIOTask was still in progress
	private boolean saveAgain = false;
	// shopkeepers that got deleted during the last async save:
	private final List<AbstractShopkeeper> shopkeepersToDelete = new ArrayList<>();
	// number of shopkeepers whose data got removed since the last save:
	private int deletedShopkeepersCount = 0;

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// start save task:
		if (!Settings.saveInstantly) {
			this.startSaveTask();
		}
	}

	public void onDisable() {
		// reset a few things:
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
			shopkeepersToDelete.add(shopkeeper);
		} else {
			String key = String.valueOf(shopkeeper.getId());
			saveData.set(key, null);
			saveDataBuffer.set(key, null);
			deletedShopkeepersCount++;
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
				continue; // skip this shopkeeper
			}

			String shopTypeString = shopkeeperSection.getString("type");
			// convert legacy shop type identifiers:
			if (shopTypeString != null && shopTypeString.equalsIgnoreCase("player")) {
				shopTypeString = "sell";
			}

			AbstractShopType<?> shopType = plugin.getShopTypeRegistry().get(shopTypeString);
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
		// wait for any async saving to finish:
		this.waitOrAbortAsyncSave();

		// save if dirty:
		if (this.isDirty()) {
			this.saveImmediate(); // not async here
		}
	}

	private boolean isCurrentlySavingAsync() {
		return (saveIOTask != -1);
	}

	private boolean isAsyncTaskCurrentlyRunning(int taskId) {
		// BukkitScheduler#isCurrentlyRunning doesn't work correctly currently (see SPIGOT-3619)
		// TODO should be fixed with late 1.12.2
		for (BukkitWorker worker : Bukkit.getScheduler().getActiveWorkers()) {
			if (worker.getTaskId() == taskId) {
				return true;
			}
		}
		return false;
	}

	// gets run from the main thread
	// makes sure that after this method returns there is no saving going on anymore
	// if an async save has been scheduled already, but not started yet, the save will get aborted,
	// otherwise this will wait for the async save to finish
	// in either case there might be unsaved data or unhandled save requests
	private void waitOrAbortAsyncSave() {
		assert Bukkit.isPrimaryThread();

		// If the async task has not yet been passed to the executor (its still pending), we cannot wait here for it to
		// finish (because the scheduler operates on the main thread as well), instead we cancel it:
		if (this.isCurrentlySavingAsync()) {
			Bukkit.getScheduler().cancelTask(saveIOTask);
		}

		// if the saving is currently in progress, acquiring the lock will wait for it to finish:
		synchronized (SAVING_IO_LOCK) {
			// If the task has already been started (there is a worker thread for it already) but not taken the lock
			// yet, we cannot cancel it and need to give up the lock again in order for it to be able to finish.
			// We may be able request a quicker abort in this case. And if saving has already finished and only the
			// syncSavingCallback is still remaining to get run, this flag signalizes that we don't want any new saving
			// requests (needs to be synchronized here to get correctly propagated):
			abortSave = true;
			while (saveResult.state == SaveResult.State.NOT_YET_STARTED && this.isAsyncTaskCurrentlyRunning(saveIOTask)) {
				try {
					// release the lock, for the async task to be able to operate,
					// the async task has to notify us once it has finished:
					SAVING_IO_LOCK.wait();
				} catch (InterruptedException e) {
					// we are not interruptible, but we restore the interruption status:
					Thread.currentThread().interrupt();
				}
			}
			// at this point the async task was successfully cancelled or has finished execution
		}

		// manually run the callback of the previous save here, to complete the previous save before continuing:
		if (syncSavingCallback != null) {
			syncSavingCallback.run();
		}

		// reset abort flag:
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

		// stop current delayed save task:
		if (delayedSaveTaskId != -1) {
			Bukkit.getScheduler().cancelTask(delayedSaveTaskId);
			delayedSaveTaskId = -1;
		}

		// is another async save task already running?
		if (this.isCurrentlySavingAsync()) {
			if (async) {
				// set flag which triggers a new save once that current task is done:
				saveAgain = true;
				currentlyProcessingSave = false;
				return;
			} else {
				// wait for any async saving to finish:
				this.waitOrAbortAsyncSave();
			}
		}

		// keep track of statistics and information about this saving attempt:
		saveResult.reset();
		saveResult.async = async;
		saveResult.startTime = System.currentTimeMillis();

		// store data of dirty shopkeepers into memory configuration:
		saveResult.dirtyShopkeepersCount = 0;
		for (AbstractShopkeeper shopkeeper : this.getShopkeeperRegistry().getAllShopkeepers()) {
			if (!shopkeeper.isDirty()) {
				continue; // assume storage data is still up-to-date
			}
			saveResult.dirtyShopkeepersCount++;

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

		// store number of deleted shopkeepers (for debugging purposes):
		saveResult.deletedShopkeepersCount = deletedShopkeepersCount;
		deletedShopkeepersCount = 0;

		// time to store shopkeeper data in memory configuration:
		saveResult.packingDuration = System.currentTimeMillis() - saveResult.startTime;

		// note: the dirty flag might get reverted again after saving, if saving failed
		// however, the flag gets reset here (and not just after successful saving), so that any saving requests that
		// arrive in the meantime get noticed and can cause another save later:
		dirty = false;

		// gets run on the main thread after the save has been completed or aborted (counts as failure):
		// note: this needs to be a new runnable (cannot be a lambda), in order to be able to reliable use the objects
		// identify to identify whether the callback has already been run
		syncSavingCallback = new Runnable() {
			@Override
			public void run() {
				// abort if this callback has already been run manually (ex. when waiting for saving to finish):
				// checking identify instead of null here, because if the callback has already been run manually,
				// another save might have been prepared already while this task was still pending to get run
				if (syncSavingCallback != this) {
					return;
				}
				syncSavingCallback = null;

				// reset save task id:
				// it's important that this gets reset inside this sync task, otherwise other save request might get
				// prepared before this save has been fully handled
				saveIOTask = -1;

				// note: the save result state might still be NOT_YET_STARTED, if the saving task got cancelled before
				// it could run

				// mark all shopkeepers as dirty again, whose data we were not able to save:
				if (saveResult.state != SaveResult.State.SUCCESS) { // failure or aborted or cancelled
					if (!savingShopkeepers.isEmpty()) {
						for (AbstractShopkeeper shopkeeper : savingShopkeepers) {
							shopkeeper.markDirty();
						}
						// request another delayed save (if there isn't an abort request):
						if (!abortSave) {
							saveDelayed();
						}
					}

					// restore number of deleted shopkeepers:
					deletedShopkeepersCount = saveResult.deletedShopkeepersCount;
				}
				savingShopkeepers.clear();

				// remove data of shopkeepers that have been deleted during the save:
				for (AbstractShopkeeper deletedShopkeeper : shopkeepersToDelete) {
					clearShopkeeperData(deletedShopkeeper);
				}
				shopkeepersToDelete.clear();

				// if not aborted / cancelled:
				if (saveResult.state == SaveResult.State.SUCCESS || saveResult.state == SaveResult.State.FAILURE) {
					// print debug info:
					saveResult.printDebugInfo();

					// saving failed?
					if (saveResult.state != SaveResult.State.SUCCESS) {
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
				}

				if (async) {
					// did we get another saveReal-request in the meantime?
					if (saveAgain) {
						saveAgain = false;
						// trigger another save with the latest data (if there isn't an abort request):
						if (!abortSave) {
							saveNow();
						}
					}
				}
			}
		};
		// called possibly async:
		Runnable savingCallback = () -> {
			// ensure that we continue on main thread:
			// this gets omitted here if the plugin has been disabled during an async save,
			// in which case the callback gets manually run from the onDisable handling on the main thread
			SchedulerUtils.runOnMainThreadOrOmit(plugin, syncSavingCallback);
		};

		if (!async) {
			// sync file io:
			this.saveDataToFile(saveDataBuffer, savingCallback);
		} else {
			// async file io:
			final long asyncTaskSubmittedTime = System.currentTimeMillis();
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				// note: if the task gets cancelled, this never gets run (everything that always needs to happen, has to
				// be placed or copied into the callback as well)
				saveResult.asyncTaskDelay = System.currentTimeMillis() - asyncTaskSubmittedTime;
				// synchronization, so that only one thread at a time attempts to mess with the save files
				final long ioLockStartTime = System.currentTimeMillis();
				synchronized (SAVING_IO_LOCK) {
					saveResult.ioLockAcquireDuration = System.currentTimeMillis() - ioLockStartTime;
					// did we get an abort request? -> skip saving:
					if (abortSave) {
						saveResult.state = SaveResult.State.ABORTED;
						// if aborted, the syncSavingCallback needs to be run manually
					} else {
						// actual saving IO:
						this.saveDataToFile(saveDataBuffer, savingCallback);
						assert saveResult.state == SaveResult.State.SUCCESS || saveResult.state == SaveResult.State.FAILURE;
					}
					// async saving is over:
					// it's important that the save result state gets set before the lock is released, because otherwise
					// we cannot differentiate between whether the running task has already finished or is still going
					// to acquire the lock
					assert saveResult.state != SaveResult.State.NOT_YET_STARTED;

					// notify all possibly waiting threads:
					SAVING_IO_LOCK.notifyAll();
				}
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
					saveResult.state = SaveResult.State.FAILURE;
					Log.severe("Saving failed! Save data might be lost! :(");
					break;
				}
			} else {
				// saving was successful:
				saveResult.state = SaveResult.State.SUCCESS;
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

	private static class SaveResult {

		// note: synchronization for those values is not needed, because they get synchronized externally before getting
		// used, either by the bukkit scheduler (when starting the async task and when going back to the main thread by
		// starting a sync task), or via synchronization of the SAVING_IO_LOCK

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
			// all other attributes get set appropriately before getting used
		}

		public void printDebugInfo() {
			Log.debug("Saved shopkeeper data (" + totalDuration + "ms (Data packing ("
					+ dirtyShopkeepersCount + " dirty, " + deletedShopkeepersCount + " deleted): " + packingDuration + "ms, "
					+ (async ? "AsyncTask delay: " + asyncTaskDelay + "ms, " : "")
					+ ((ioLockAcquireDuration > 1) ? "IO lock delay: " + ioLockAcquireDuration + "ms, " : "")
					+ (async ? "Async " : "Sync ") + "IO: " + ioDuration + "ms))"
					+ ((state == State.FAILURE) ? ": Saving failed!" : ""));
		}
	}
}
