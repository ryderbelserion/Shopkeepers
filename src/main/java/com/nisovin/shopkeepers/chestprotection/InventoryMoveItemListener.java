package com.nisovin.shopkeepers.chestprotection;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
		if (this.isProtectedInventory(event.getSource()) || this.isProtectedInventory(event.getDestination())) {
			event.setCancelled(true);
		}
	}

	private boolean isProtectedInventory(Inventory inventory) {
		if (inventory == null) return false;
		InventoryHolder holder = inventory.getHolder();
		return this.isProtectedInventoryHolder(holder);
	}

	private boolean isProtectedInventoryHolder(InventoryHolder inventoryHolder) {
		if (inventoryHolder == null) return false;
		if (inventoryHolder instanceof DoubleChest) {
			DoubleChest doubleChest = (DoubleChest) inventoryHolder;
			return (this.isProtectedInventoryHolder(doubleChest.getLeftSide())
					|| this.isProtectedInventoryHolder(doubleChest.getRightSide()));
		} else if (inventoryHolder instanceof Chest) {
			Block block = ((Chest) inventoryHolder).getBlock();
			return protectedChests.isChestProtected(block, null);
		}
		return false;
	}
}
