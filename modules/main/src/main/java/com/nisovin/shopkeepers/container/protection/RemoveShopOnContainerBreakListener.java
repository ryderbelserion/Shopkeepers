package com.nisovin.shopkeepers.container.protection;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.container.ShopContainers;

class RemoveShopOnContainerBreakListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final RemoveShopOnContainerBreak removeShopOnContainerBreak;

	RemoveShopOnContainerBreakListener(
			SKShopkeepersPlugin plugin,
			RemoveShopOnContainerBreak removeShopOnContainerBreak
	) {
		assert plugin != null && removeShopOnContainerBreak != null;
		this.plugin = plugin;
		this.removeShopOnContainerBreak = removeShopOnContainerBreak;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (ShopContainers.isSupportedContainer(block.getType())
				&& removeShopOnContainerBreak.handleBlockBreakage(block)) {
			plugin.getShopkeeperStorage().save();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		List<Block> blockList = event.blockList();
		removeShopOnContainerBreak.handleBlocksBreakage(blockList);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		List<Block> blockList = event.blockList();
		removeShopOnContainerBreak.handleBlocksBreakage(blockList);
	}
}
