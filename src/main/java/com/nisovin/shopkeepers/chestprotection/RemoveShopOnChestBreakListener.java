package com.nisovin.shopkeepers.chestprotection;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.util.ItemUtils;

class RemoveShopOnChestBreakListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final RemoveShopOnChestBreak removeShopOnChestBreak;

	RemoveShopOnChestBreakListener(SKShopkeepersPlugin plugin, RemoveShopOnChestBreak removeShopOnChestBreak) {
		assert plugin != null && removeShopOnChestBreak != null;
		this.plugin = plugin;
		this.removeShopOnChestBreak = removeShopOnChestBreak;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isChest(block.getType()) && removeShopOnChestBreak.handleBlockBreakage(block)) {
			plugin.getShopkeeperStorage().save();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		removeShopOnChestBreak.handleBlocksBreakage(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		removeShopOnChestBreak.handleBlocksBreakage(event.blockList());
	}
}
