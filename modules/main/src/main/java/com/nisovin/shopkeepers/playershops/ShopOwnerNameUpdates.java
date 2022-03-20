package com.nisovin.shopkeepers.playershops;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class ShopOwnerNameUpdates implements Listener {

	private final SKShopkeepersPlugin plugin;

	public ShopOwnerNameUpdates(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Update the owner information of all shops owned by players that are already online:
		for (Player player : Bukkit.getOnlinePlayers()) {
			assert player != null;
			if (CitizensUtils.isNPC(player)) continue;
			UUID playerId = player.getUniqueId();
			String playerName = Unsafe.assertNonNull(player.getName());
			this.updateShopkeepersForPlayer(playerId, playerName);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!player.isOnline()) {
			// Ignore if the player is already no longer online (maybe the player was kicked):
			return;
		}

		// Update the owner information of all shops owned by this player:
		String playerName = Unsafe.assertNonNull(player.getName());
		this.updateShopkeepersForPlayer(player.getUniqueId(), playerName);
	}

	// Updates owner names for the shopkeepers of the specified player:
	private void updateShopkeepersForPlayer(UUID playerId, String playerName) {
		Log.debug(DebugOptions.ownerNameUpdates, () -> "Updating shopkeeper owner names for: "
				+ TextUtils.getPlayerString(playerName, playerId));

		boolean dirty = false;
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		for (PlayerShopkeeper playerShop : shopkeeperRegistry.getPlayerShopkeepersByOwner(playerId)) {
			String ownerName = playerShop.getOwnerName();
			if (!ownerName.equals(playerName)) {
				// Update the stored name, because the player must have changed it:
				Log.debug(DebugOptions.ownerNameUpdates, () -> playerShop.getLogPrefix()
						+ "Updating owner name '" + ownerName + "' to '" + playerName + "'.");
				playerShop.setOwner(playerId, playerName);
				dirty = true;
			} else if (!dirty) { // Dirty if we already found a shop with mismatching name
				// The stored owner name matches the player's current name.
				// Since we assume that the stored owner names among all shopkeepers are consistent,
				// we can abort checking the remaining shops of this player.
				Log.debug(DebugOptions.ownerNameUpdates, () -> playerShop.getLogPrefix()
						+ "Owner name '" + ownerName + "' is up-to-date. "
						+ "Assuming the owner names of all shopkeepers are consistent, "
						+ "we skip checking all other shopkeepers.");
				return;
			}
		}

		// Save:
		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}
}
