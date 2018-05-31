package com.nisovin.shopkeepers.api.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Utility functions related to materials, items and inventories.
 */
public class ItemUtils {

	private ItemUtils() {
	}

	// itemstack utilities:

	public static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
	}

	public static ItemStack getNullIfEmpty(ItemStack item) {
		return isEmpty(item) ? null : item;
	}
}
