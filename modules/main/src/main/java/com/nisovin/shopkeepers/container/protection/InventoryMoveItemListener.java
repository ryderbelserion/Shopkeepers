package com.nisovin.shopkeepers.container.protection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.container.ShopContainers;

/**
 * Prevents item movement from/to protected containers. Can be disabled via a config setting.
 */
class InventoryMoveItemListener implements Listener {

	private final ProtectedContainers protectedContainers;

	InventoryMoveItemListener(ProtectedContainers protectedContainers) {
		this.protectedContainers = protectedContainers;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		assert event.getSource() != null && event.getDestination() != null;
		if (this.isProtectedInventory(event.getSource())
				|| this.isProtectedInventory(event.getDestination())) {
			event.setCancelled(true);
		}
	}

	private boolean isProtectedInventory(Inventory inventory) {
		assert inventory != null;
		// Note: We avoid calling Inventory#getHolder here for performance reasons. For block
		// inventories this creates a snapshot of the block's BlockState.
		Location inventoryLocation = inventory.getLocation(); // can be null
		if (inventoryLocation == null) return false;
		Block block = inventoryLocation.getBlock(); // not null
		if (!ShopContainers.isSupportedContainer(block.getType())) return false;
		// Also checks for protected connected chests (double chests):
		return protectedContainers.isContainerProtected(block, null);
	}
}
