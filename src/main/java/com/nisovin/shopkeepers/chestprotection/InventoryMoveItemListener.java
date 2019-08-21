package com.nisovin.shopkeepers.chestprotection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Prevents item movement from/to protected shop chests. Can be disabled via a config setting.
 */
class InventoryMoveItemListener implements Listener {

	private final ProtectedChests protectedChests;

	InventoryMoveItemListener(ProtectedChests protectedChests) {
		this.protectedChests = protectedChests;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		// source and destination inventories are not null
		if (this.isProtectedInventory(event.getSource()) || this.isProtectedInventory(event.getDestination())) {
			event.setCancelled(true);
		}
	}

	private boolean isProtectedInventory(Inventory inventory) {
		assert inventory != null;
		// Note: We are avoiding calling Inventory#getHolder here for performance reasons
		Location inventoryLocation = inventory.getLocation(); // can be null
		if (inventoryLocation == null) return false;
		Block block = inventoryLocation.getBlock(); // not null
		if (!ItemUtils.isChest(block.getType())) return false;
		// also checks for protected connected chests (double chests):
		return protectedChests.isChestProtected(block, null);
	}
}
