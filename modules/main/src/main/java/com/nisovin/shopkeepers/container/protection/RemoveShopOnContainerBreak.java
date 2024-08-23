package com.nisovin.shopkeepers.container.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;

public class RemoveShopOnContainerBreak {

	private final SKShopkeepersPlugin plugin;
	private final ProtectedContainers protectedContainers;
	private final RemoveShopOnContainerBreakListener removeShopOnContainerBreakListener;

	public RemoveShopOnContainerBreak(
			SKShopkeepersPlugin plugin,
			ProtectedContainers protectedContainers
	) {
		this.plugin = plugin;
		this.protectedContainers = protectedContainers;
		removeShopOnContainerBreakListener = new RemoveShopOnContainerBreakListener(
				plugin,
				Unsafe.initialized(this)
		);
	}

	public void onEnable() {
		if (Settings.deleteShopkeeperOnBreakContainer) {
			Bukkit.getPluginManager().registerEvents(removeShopOnContainerBreakListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(removeShopOnContainerBreakListener);
	}

	// Does not trigger saving on its own, returns true if there were shopkeepers using the
	// container, that got removed now.
	// Does not check the delete-shopkeeper-on-break-container setting, this has to be checked by
	// clients beforehand.
	// Does not check whether the block is still a valid container type.
	public boolean handleBlockBreakage(Block block) {
		List<? extends PlayerShopkeeper> shopkeepers = protectedContainers.getShopkeepers(block);
		if (shopkeepers.isEmpty()) return false;

		// Copy to deal with concurrent modifications:
		for (PlayerShopkeeper shopkeeper : shopkeepers.toArray(new PlayerShopkeeper[0])) {
			if (!shopkeeper.isValid()) continue; // Skip if no longer valid
			// Return the shop creation item for player shopkeepers:
			if (Settings.deletingPlayerShopReturnsCreationItem) {
				ItemStack shopCreationItem = ShopCreationItem.create();
				block.getWorld().dropItemNaturally(block.getLocation(), shopCreationItem);
			}
			// Note: We do not pass the player responsible for breaking the container here, because
			// we cannot determine the player in all situations anyway (e.g. if a player indirectly
			// breaks the container by causing an explosion).
			shopkeeper.delete();
		}
		return true;
	}

	public void handleBlocksBreakage(List<? extends Block> blockList) {
		boolean dirty = false;
		for (Block block : blockList) {
			if (!ShopContainers.isSupportedContainer(block.getType())) continue;
			if (this.handleBlockBreakage(block)) {
				dirty = true;
			}

		}
		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}
}
