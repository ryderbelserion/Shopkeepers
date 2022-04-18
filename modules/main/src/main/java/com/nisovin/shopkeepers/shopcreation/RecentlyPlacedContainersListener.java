package com.nisovin.shopkeepers.shopcreation;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.shopkeepers.container.ShopContainers;

class RecentlyPlacedContainersListener implements Listener {

	private final ContainerSelection containerSelection;

	RecentlyPlacedContainersListener(ContainerSelection containerSelection) {
		this.containerSelection = containerSelection;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (ShopContainers.isSupportedContainer(block.getType())) {
			containerSelection.addRecentlyPlacedContainer(event.getPlayer(), block);
		}
	}
}
