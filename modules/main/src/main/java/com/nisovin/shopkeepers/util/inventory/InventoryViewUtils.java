package com.nisovin.shopkeepers.util.inventory;

import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;

/**
 * {@link InventoryView} related helpers.
 */
public final class InventoryViewUtils {

	public static boolean isTopInventory(InventoryView view, int rawSlot) {
		return rawSlot >= 0 && rawSlot < view.getTopInventory().getSize();
	}

	public static boolean isPlayerInventory(InventoryView view, int rawSlot) {
		if (rawSlot < view.getTopInventory().getSize()) return false;

		SlotType slotType = view.getSlotType(rawSlot);
		return slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR;
	}

	private InventoryViewUtils() {
	}
}
