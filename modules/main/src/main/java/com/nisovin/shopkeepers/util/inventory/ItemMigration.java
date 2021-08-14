package com.nisovin.shopkeepers.util.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

public class ItemMigration {

	private static Inventory DUMMY_INVENTORY = null;

	private ItemMigration() {
	}

	// Use oldItemStack.isSimilar(migratedItemStack) to test if the item was migrated.
	public static ItemStack migrateItemStack(@ReadOnly ItemStack itemStack) {
		if (itemStack == null) return null;
		if (DUMMY_INVENTORY == null) {
			DUMMY_INVENTORY = Bukkit.createInventory(null, 9);
		}

		// Inserting an ItemStack into a Minecraft inventory will convert it to a corresponding nms.ItemStack and
		// thereby trigger any Minecraft data migrations for the ItemStack.
		DUMMY_INVENTORY.setItem(0, itemStack);
		ItemStack convertedItemStack = DUMMY_INVENTORY.getItem(0);
		DUMMY_INVENTORY.setItem(0, null);
		return convertedItemStack;
	}

	public static UnmodifiableItemStack migrateItemStack(UnmodifiableItemStack itemStack) {
		return UnmodifiableItemStack.of(migrateItemStack(ItemUtils.asItemStackOrNull(itemStack)));
	}
}
