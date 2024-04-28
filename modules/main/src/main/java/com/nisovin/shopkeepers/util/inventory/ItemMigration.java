package com.nisovin.shopkeepers.util.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Handles Minecraft-native item data migrations.
 */
public final class ItemMigration {

	private static @Nullable Inventory DUMMY_INVENTORY = null;

	// Use oldItemStack.isSimilar(migratedItemStack) to test if the item was migrated.
	public static @Nullable ItemStack migrateItemStack(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return null;
		Inventory inventory = DUMMY_INVENTORY;
		if (inventory == null) {
			inventory = Bukkit.createInventory(null, 9);
			DUMMY_INVENTORY = inventory;
		}
		assert inventory != null;

		// Inserting an ItemStack into a Minecraft inventory will convert it to a corresponding
		// nms.ItemStack and thereby trigger any Minecraft data migrations for the ItemStack.
		inventory.setItem(0, itemStack);
		ItemStack convertedItemStack = inventory.getItem(0);
		inventory.setItem(0, null);
		return convertedItemStack;
	}

	public static @Nullable UnmodifiableItemStack migrateItemStack(
			@Nullable UnmodifiableItemStack itemStack
	) {
		return UnmodifiableItemStack.of(migrateItemStack(ItemUtils.asItemStackOrNull(itemStack)));
	}

	public static ItemStack migrateNonNullItemStack(@ReadOnly ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemStack migrated = migrateItemStack(itemStack);
		return Validate.State.notNull(migrated, () -> "Migrated ItemStack is null! Original: "
				+ itemStack);
	}

	private ItemMigration() {
	}
}
