package com.nisovin.shopkeepers.naming;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

class ShopNamingListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperNaming shopkeeperNaming;

	ShopNamingListener(SKShopkeepersPlugin plugin, ShopkeeperNaming shopkeeperNaming) {
		this.plugin = plugin;
		this.shopkeeperNaming = shopkeeperNaming;
	}

	// SHOPKEEPER NAMING

	// ignoreCancelled = false: We process the chat event in all cases (even if the player has been muted, etc.).
	// Priority LOWEST: We intend to cancel the event as early as possible, so that other plugins can ignore it.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Shopkeeper shopkeeper = shopkeeperNaming.endNaming(player);
		if (shopkeeper == null) return;

		event.setCancelled(true);
		String newName = event.getMessage().trim();
		Bukkit.getScheduler().runTask(plugin, () -> shopkeeperNaming.requestNameChange(player, shopkeeper, newName));
	}
}
