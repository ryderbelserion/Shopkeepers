package com.nisovin.shopkeepers.chestprotection;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
		if (!ItemUtils.isChest(block.getType())) return;

		List<PlayerShopkeeper> shopkeepers = protectedChests.getShopkeeperOwnersOfChest(block);
		if (shopkeepers.isEmpty()) return;

		for (PlayerShopkeeper shopkeeper : shopkeepers) {
			// return creation item for player shopkeepers:
			if (Settings.deletingPlayerShopReturnsCreationItem) {
				ItemStack shopCreationItem = Settings.createShopCreationItem();
				block.getWorld().dropItemNaturally(block.getLocation(), shopCreationItem);
			}
			shopkeeper.delete();
		}
		plugin.getShopkeeperStorage().save();
	}
}
