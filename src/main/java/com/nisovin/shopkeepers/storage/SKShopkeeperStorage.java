package com.nisovin.shopkeepers.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.AbstractShopType;
import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.shoptypes.SKDefaultShopTypes;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.StringUtils;

public class SKShopkeeperStorage implements ShopkeeperStorage {

	private final SKShopkeepersPlugin plugin;
	// flag to (temporary) turn off saving
	private boolean savingDisabled = false;
	private long lastSavingErrorMsgTimeStamp = 0L;
	private boolean dirty = false;
	private int delayedSaveTaskId = -1;
	// the task which performs async file io during a save
	private int saveIOTask = -1;
	// determines if there was another saveReal()-request while the saveIOTask was still in progress
	private boolean saveRealAgain = false;

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void reset() {
		savingDisabled = false;
		lastSavingErrorMsgTimeStamp = 0L;
		dirty = false;
		delayedSaveTaskId = -1;
		saveIOTask = -1;
		saveRealAgain = false;
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

	private static class SaveResult {

		// note: synchronization for those values is not needed, because they don't get modified by the main and async
		// task at the same time, and because the bukkit scheduler already includes memory-barriers when starting the
		// async IO task and when going back to the main thread by starting a sync task, so changes become visible to
		// each thread involved

		private boolean async;
		private long startTime;
		private long packingDuration;
		private long asyncTaskDelay;
		private long ioLockAcquireDuration;
		private long ioDuration;
		private long totalDuration;
		private boolean success;

		public void printDebugInfo() {
			Log.debug("Saved shopkeeper data (" + totalDuration + "ms (Data packing: " + packingDuration + "ms, "
					+ (async ? "AsyncTask delay: " + asyncTaskDelay + "ms, " : "")
					+ ((ioLockAcquireDuration > 1) ? "IO lock delay: " + ioLockAcquireDuration + "ms, " : "")
					+ (async ? "Async " : "Sync ") + "IO: " + ioDuration + "ms))"
					+ (success ? "" : ": Saving failed!"));
		}
	}

	private File getSaveFile() {
		return new File(plugin.getDataFolder(), "save.yml");
	}

	private File getTempSaveFile() {
		File saveFile = this.getSaveFile();
		return new File(saveFile.getParentFile(), saveFile.getName() + ".temp");
	}

	// returns true on success
	public boolean load() {
		try {
			return this._load();
		} catch (Exception e) {
			Log.getLogger().log(Level.SEVERE, "Something completely unexpected went wrong during the loading of the saved shopkeepers data!", e);
			return false; // error
		}
	}

	// returns true on success, and false if there was some issue during loading
	private boolean _load() {
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

		YamlConfiguration shopkeepersConfig = new YamlConfiguration();
		try {
			if (!StringUtils.isEmpty(Settings.fileEncoding)) {
				// load with specified charset:
				try (	FileInputStream stream = new FileInputStream(saveFile);
						InputStreamReader reader = new InputStreamReader(stream, Settings.fileEncoding)) {
					shopkeepersConfig.load(reader);
				}
			} else {
				// load with default charset handling:
				shopkeepersConfig.load(saveFile);
			}
		} catch (Exception e) {
			Log.severe("Failed to load save file!");
			e.printStackTrace();
			return false; // disable without save
		}

		Set<String> ids = shopkeepersConfig.getKeys(false);
		Log.info("Loading data of " + ids.size() + " shopkeepers..");
		for (String id : ids) {
			ConfigurationSection shopkeeperSection = shopkeepersConfig.getConfigurationSection(id);
			if (shopkeeperSection == null) {
				Log.warning("Failed to load shopkeeper '" + id + "': Invalid config section!");
				continue;
			}
			AbstractShopType<?> shopType = plugin.getShopTypeRegistry().get(shopkeeperSection.getString("type"));
			// unknown shop type
			if (shopType == null) {
				// got an owner entry? -> default to normal player shop type
				if (shopkeeperSection.contains("owner")) {
					Log.warning("No valid shop type specified for shopkeeper '" + id + "': defaulting to "
							+ SKDefaultShopTypes.PLAYER_NORMAL().getIdentifier());
					shopType = SKDefaultShopTypes.PLAYER_NORMAL();
				} else {
					// no valid shop type given..
					Log.severe("Failed to load shopkeeper '" + id + "': unknown type");
					return false; // disable without save
				}
			}

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

			// load shopkeeper:
			try {
				Shopkeeper shopkeeper = shopType.loadShopkeeper(shopkeeperSection);
				if (shopkeeper == null) {
					throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
				}
			} catch (Exception e) {
				Log.severe("Failed to load shopkeeper '" + id + "': " + e.getMessage());
				e.printStackTrace();
				return false; // disable without save
			}
		}
		return true;
	}

	@Override
	public void save() {
		if (Settings.saveInstantly) {
			this.saveReal();
		} else {
			dirty = true;
		}
	}

	@Override
	public void saveDelayed() {
		dirty = true;
		if (Settings.saveInstantly && delayedSaveTaskId == -1) {
			delayedSaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
				if (dirty) {
					saveReal();
				}
				delayedSaveTaskId = -1;
			}, 600).getTaskId(); // 30 seconds delay
		}
	}

	@Override
	public void saveReal() {
		this.saveReal(true);
	}

	// should only get called with parameter async=false on disable:
	public void saveReal(boolean async) {
		if (savingDisabled) {
			Log.warning("Skipping saving because it got disabled.");
			return;
		}

		// is another async save task already running?
		if (async && saveIOTask != -1) {
			// set flag which triggers a new save once that current task is done:
			saveRealAgain = true;
			return;
		}

		// keeps track of statistics and information about this saving attempt:
		SaveResult saveResult = new SaveResult();
		saveResult.async = async;
		saveResult.startTime = System.currentTimeMillis();

		// store shopkeeper data into memory configuration:
		YamlConfiguration config = new YamlConfiguration();
		int counter = 1;
		for (AbstractShopkeeper shopkeeper : plugin.getAllShopkeepers()) {
			String sectionKey = String.valueOf(counter++);
			ConfigurationSection section = config.createSection(sectionKey);
			try {
				shopkeeper.save(section);
			} catch (Exception e) {
				// error while saving shopkeeper data:
				// skip this shopkeeper and print warning + stacktrace:
				config.set(sectionKey, null);
				Log.warning("Couldn't save shopkeeper '" + shopkeeper.getUniqueId() + "' at " + shopkeeper.getPositionString() + "!");
				e.printStackTrace();
			}
		}
		// time to store shopkeeper data in memory configuration:
		saveResult.packingDuration = System.currentTimeMillis() - saveResult.startTime;

		// note: the dirty flag might get reverted again after saving, if saving failed
		// however, the flag gets reset here (and not just after successful saving), so that any saving requests that
		// arrive in the meantime get noticed and can cause another save later:
		dirty = false;

		// called sync:
		Runnable syncSavingCallback = () -> {
			// print debug info:
			saveResult.printDebugInfo();

			// saving failed?
			if (!saveResult.success) {
				// mark as dirty, as there is potentially unsaved data, and request another delayed save:
				dirty = true;
				saveDelayed();

				// inform admins about saving issue:
				// 4 min error message throttle (slightly less than the saving interval)
				if (Math.abs(System.currentTimeMillis() - lastSavingErrorMsgTimeStamp) > (4 * 60 * 1000L)) {
					lastSavingErrorMsgTimeStamp = System.currentTimeMillis();
					String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED + "Saving shop data failed! Please check out the server log(s) and look into the issue!";
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player.hasPermission(ShopkeepersPlugin.ADMIN_PERMISSION)) {
							player.sendMessage(errorMsg);
						}
					}
				}
			}

			if (async) {
				saveIOTask = -1;

				// did we get another request to saveReal() in the meantime?
				if (saveRealAgain) {
					// trigger another full save with latest data:
					saveRealAgain = false;
					saveReal();
				}
			}
		};
		// called possibly async:
		Runnable savingCallback = () -> {
			// ensure that we continue on main thread:
			SchedulerUtils.runOnMainThreadOrOmit(plugin, syncSavingCallback);
		};

		if (!async) {
			// sync file io:
			this.saveDataToFile(config, saveResult, savingCallback);
		} else {
			// async file io:
			final long asyncTaskSubmittedTime = System.currentTimeMillis();
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				saveResult.asyncTaskDelay = System.currentTimeMillis() - asyncTaskSubmittedTime;
				saveDataToFile(config, saveResult, savingCallback);
			}).getTaskId();
		}
	}

	// max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;
	private static final Object SAVING_IO_LOCK = new Object();

	// can be run async and sync
	private void saveDataToFile(FileConfiguration config, SaveResult saveResult, Runnable callback) {
		assert config != null && saveResult != null;

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
		}
		// file IO over

		// run callback:
		if (callback != null) {
			callback.run();
		}
	}
}
