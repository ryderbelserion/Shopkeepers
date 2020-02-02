package com.nisovin.shopkeepers.chestprotection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;

public class RemoveShopOnChestBreak {

	private final SKShopkeepersPlugin plugin;
	private final ProtectedChests protectedChests;
	private final RemoveShopOnChestBreakListener removeShopOnChestBreakListener;

	public RemoveShopOnChestBreak(SKShopkeepersPlugin plugin, ProtectedChests protectedChests) {
		this.plugin = plugin;
		this.protectedChests = protectedChests;
		removeShopOnChestBreakListener = new RemoveShopOnChestBreakListener(plugin, this);
	}

	public void onEnable() {
		if (Settings.deleteShopkeeperOnBreakChest) {
			Bukkit.getPluginManager().registerEvents(removeShopOnChestBreakListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(removeShopOnChestBreakListener);
	}

	// does not trigger saving on its own, returns true if there were shopkeepers using the chest, that got removed now
	// does not check the delete-shopkeeper-on-break-chest setting, this has to be checked by clients beforehand
	public boolean handleBlockBreakage(Block block) {
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

	public void handleBlocksBreakage(List<Block> blockList) {
		boolean dirty = false;
		for (Block block : blockList) {
			if (ItemUtils.isChest(block.getType()) && this.handleBlockBreakage(block)) {
				dirty = true;
			}
		}
		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}
}
