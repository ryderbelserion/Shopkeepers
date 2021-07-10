package com.nisovin.shopkeepers.playershops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerInactiveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

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

		// Get all shop owners:
		// This initially contains all shop owners (and maps to no shopkeepers), and is then pruned.
		Map<User, List<PlayerShopkeeper>> shopsByInactivePlayers = new HashMap<>();
		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
			// In this first step, we are only interested in the existing shop owners, and therefore don't store the
			// shopkeepers. We later gather the shopkeepers of only the inactive shop owners.
			// We insert the empty list here instead of null, because the subsequent code needs to be able to
			// differentiate between there being a mapping for a specific user versus the user being mapped to null.
			shopsByInactivePlayers.put(playerShop.getOwnerUser(), Collections.emptyList());
		});
		if (shopsByInactivePlayers.isEmpty()) {
			return; // There are no player shops
		}

		// Retrieve the OfflinePlayers and their 'last played' times asynchronously:
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			// Note: We don't use #removeIf(Predicate) here, because the implementation may evaluate the Predicate
			// multiple times (which may be costly).
			Iterator<User> shopOwnersIterator = shopsByInactivePlayers.keySet().iterator();
			while (shopOwnersIterator.hasNext()) {
				User shopOwner = shopOwnersIterator.next();
				if (!isInactive(shopOwner)) {
					shopOwnersIterator.remove();
				}
			}
			if (shopsByInactivePlayers.isEmpty()) {
				return; // No inactive players found
			}

			// Continue in main thread:
			SchedulerUtils.runTaskOrOmit(plugin, () -> {
				// Get the shopkeepers owned by the inactive players:
				shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
					// If the shop is owned by an inactive player, remember it for removal:
					User shopOwner = playerShop.getOwnerUser();
					List<PlayerShopkeeper> ownedShopkeepers = shopsByInactivePlayers.computeIfPresent(shopOwner, (user, shopkeepers) -> {
						// Replace the initial unmodifiable empty list with a modifiable one:
						if (shopkeepers.isEmpty()) {
							shopkeepers = new ArrayList<>();
						}
						return shopkeepers;
					});
					if (ownedShopkeepers != null) {
						ownedShopkeepers.add(playerShop);
					}
				});

				// Process the inactive players and their shopkeepers:
				if (!shopsByInactivePlayers.isEmpty()) {
					shopsByInactivePlayers.entrySet().forEach(entry -> {
						User user = entry.getKey();
						List<PlayerShopkeeper> shopkeepers = entry.getValue();
						int originalShopkeepersCount = shopkeepers.size();
						assert !shopkeepers.isEmpty();

						// Call event:
						PlayerInactiveEvent event = new PlayerInactiveEvent(user, shopkeepers);
						Bukkit.getPluginManager().callEvent(event);
						if (event.isCancelled() || shopkeepers.isEmpty()) {
							Log.debug("Ignoring inactive player " + TextUtils.getPlayerString(user) + " and their " + originalShopkeepersCount
									+ (shopkeepers.size() == originalShopkeepersCount ? "" : " (reduced to " + shopkeepers.size() + ")")
									+ " shopkeepers: Cancelled by a plugin.");
						} else {
							// Delete the shopkeepers:
							shopkeepers.forEach(playerShop -> {
								Log.info("Deleting shopkeeper " + playerShop.getIdString() + " at " + playerShop.getPositionString()
										+ " owned by " + playerShop.getOwnerString() + " due to owner inactivity.");
								playerShop.delete();
							});
						}
					});

					// Save if necessary:
					plugin.getShopkeeperStorage().saveIfDirty();
				}
			});
		});
	}

	private boolean isInactive(User user) {
		OfflinePlayer offlinePlayer = user.getOfflinePlayer();
		// Some servers may delete player data files, either regularly for all players (which breaks this feature), or
		// for particular players (for example to reset or fix some issue with their data). If this is the case, we
		// cannot reliably determine when the player was last seen on the server, and therefore do not delete their
		// shopkeepers.
		if (!offlinePlayer.hasPlayedBefore()) return false;

		long lastPlayedMillis = offlinePlayer.getLastPlayed();
		if (lastPlayedMillis == 0) return false; // 0 if unknown (see reasoning above)

		long millisSinceLastPlayed = System.currentTimeMillis() - lastPlayedMillis;
		int daysSinceLastPlayed = (int) TimeUnit.MILLISECONDS.toDays(millisSinceLastPlayed);
		return (daysSinceLastPlayed >= Settings.playerShopkeeperInactiveDays);
	}
}
