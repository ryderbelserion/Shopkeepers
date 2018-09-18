package com.nisovin.shopkeepers.chestprotection;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;

class RemoveShopOnChestBreakListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final ProtectedChests protectedChests;

	RemoveShopOnChestBreakListener(SKShopkeepersPlugin plugin, ProtectedChests protectedChests) {
		this.plugin = plugin;
		this.protectedChests = protectedChests;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (this.handleShopkeeperChestBreakage(block)) {
			plugin.getShopkeeperStorage().save();
		}
	}

	// does not trigger saving on its own, returns true if there were shopkeepers using the chest, that got removed now
	private boolean handleShopkeeperChestBreakage(Block block) {
		if (!ItemUtils.isChest(block.getType())) return false;

		List<PlayerShopkeeper> shopkeepers = protectedChests.getShopkeepers(block);
		if (shopkeepers.isEmpty()) return false;

		for (PlayerShopkeeper shopkeeper : shopkeepers) {
			// return creation item for player shopkeepers:
			if (Settings.deletingPlayerShopReturnsCreationItem) {
				ItemStack shopCreationItem = Settings.createShopCreationItem();
				block.getWorld().dropItemNaturally(block.getLocation(), shopCreationItem);
			}
			shopkeeper.delete();
		}
		return true;
	}

	private void handleBlocksBreakage(List<Block> blockList) {
		boolean dirty = false;
		for (Block block : blockList) {
			if (this.handleShopkeeperChestBreakage(block)) {
				dirty = true;
			}
		}
		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		this.handleBlocksBreakage(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		this.handleBlocksBreakage(event.blockList());
	}
}
