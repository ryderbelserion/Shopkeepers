package com.nisovin.shopkeepers;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.shopkeepers.util.ItemUtils;

class RecentlyPlacedChestsListener implements Listener {

	private final SKShopkeepersPlugin plugin;

	RecentlyPlacedChestsListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isChest(block.getType())) {
			plugin.onChestPlacement(event.getPlayer(), block);
		}
	}
}
