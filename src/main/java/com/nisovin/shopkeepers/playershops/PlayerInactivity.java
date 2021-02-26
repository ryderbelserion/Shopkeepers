package com.nisovin.shopkeepers.playershops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.TimeUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Removes player shopkeepers of inactive players.
 */
public class PlayerInactivity {

	private final SKShopkeepersPlugin plugin;
	private final DeleteInactivePlayerShopsTask task;

	public PlayerInactivity(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.task = new DeleteInactivePlayerShopsTask(plugin);
	}

	public void onEnable() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return; // Feature is disabled

		// Delete inactive player shops, once shortly after plugin startup, and then periodically:
		task.start();
	}

	public void onDisable() {
		task.stop();
	}

	/**
	 * This task periodically checks for inactive players and deletes their shopkeepers.
	 * <p>
	 * The task is also run shortly after being started.
	 * <p>
	 * Since we measure player inactivity in granularity of days, and since the checking for inactive players is
	 * relatively performance-intensive, we run this task very infrequently. It is also not required that this task runs
	 * exactly in the specified interval, which is unlikely, because server lag can noticeably influence the exact
	 * interval duration. The primary purpose of this task is to account for servers that keep running for very long
	 * durations.
	 */
	private final class DeleteInactivePlayerShopsTask implements Runnable {

		// ~4 hours (can be noticeably longer if the server lags)
		private static final long INTERVAL_TICKS = 20 * 60 * 60 * 4;

		private final Plugin plugin;
		private BukkitTask task = null;

		public DeleteInactivePlayerShopsTask(Plugin plugin) {
			assert plugin != null;
			this.plugin = plugin;
		}

		public void start() {
			this.stop(); // Stop the task if it is already running

			// The task runs once shortly after start, and then periodically in large intervals:
			task = Bukkit.getScheduler().runTaskTimer(plugin, this, 5L, INTERVAL_TICKS);
		}

		public void stop() {
			if (task != null) {
				task.cancel();
				task = null;
			}
		}

		@Override
		public void run() {
			deleteInactivePlayerShops();
		}
	}

	// TODO Also add a command to manually detect and then optionally delete inactive player shops?
	public void deleteInactivePlayerShops() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return; // Feature is disabled

		Log.info("Checking for shopkeepers of inactive players.");

		Set<UUID> shopOwnerIds = new HashSet<>();
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
			shopOwnerIds.add(playerShop.getOwnerUUID());
		});
		if (shopOwnerIds.isEmpty()) {
			return; // There are no player shops
		}

		// Fetch OfflinePlayers async:
		int playerShopkeeperInactiveDays = Settings.playerShopkeeperInactiveDays;
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Set<UUID> inactiveShopOwnerIds = new HashSet<>();
			long now = System.currentTimeMillis();
			for (UUID shopOwnerId : shopOwnerIds) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(shopOwnerId);
				// Some servers may delete player data files, either regularly for all players (which breaks this
				// feature), or for particular players (for example to reset or fix some issue with their data). If this
				// is the case, we cannot reliably determine when the player was last seen on the server, and therefore
				// don't delete their player shopkeepers.
				if (!offlinePlayer.hasPlayedBefore()) continue;

				long lastPlayed = offlinePlayer.getLastPlayed();
				if (lastPlayed == 0) continue; // 0 if unknown (see reasoning above)

				long millisSinceLastPlayed = now - lastPlayed;
				int daysSinceLastPlayed = (int) TimeUtils.convert(millisSinceLastPlayed, TimeUnit.MILLISECONDS, TimeUnit.DAYS);
				if (daysSinceLastPlayed >= playerShopkeeperInactiveDays) {
					inactiveShopOwnerIds.add(shopOwnerId);
				}
			}

			if (inactiveShopOwnerIds.isEmpty()) {
				// No inactive players found:
				return;
			}

			// Continue in main thread:
			SchedulerUtils.runTaskOrOmit(plugin, () -> {
				List<PlayerShopkeeper> shopsByInactivePlayers = new ArrayList<>();
				shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
					// If the shop is owned by an inactive player, mark it for removal:
					UUID ownerId = playerShop.getOwnerUUID();
					if (inactiveShopOwnerIds.contains(ownerId)) {
						shopsByInactivePlayers.add(playerShop);
					}
				});

				// Delete those shopkeepers:
				if (!shopsByInactivePlayers.isEmpty()) {
					shopsByInactivePlayers.forEach(playerShop -> {
						Log.info("Deleting shopkeeper " + playerShop.getIdString() + " at " + playerShop.getPositionString()
								+ " owned by " + playerShop.getOwnerString() + " due to owner inactivity.");
						playerShop.delete();
					});

					// Save:
					plugin.getShopkeeperStorage().save();
				}
			});
		});
	}
}
