package com.nisovin.shopkeepers.shopcreation;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.shopkeepers.util.ItemUtils;

class RecentlyPlacedChestsListener implements Listener {

	private final ShopkeeperCreation shopkeeperCreation;

	RecentlyPlacedChestsListener(ShopkeeperCreation shopkeeperCreation) {
		this.shopkeeperCreation = shopkeeperCreation;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isChest(block.getType())) {
			shopkeeperCreation.addRecentlyPlacedChest(event.getPlayer(), block);
		}
	}
}
