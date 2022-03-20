package com.nisovin.shopkeepers.itemconversion;

import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemSerialization;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class ItemConversion {

	// Converts the given ItemStack to conform to Spigot's internal data format by running it
	// through Spigot's item de/serialization. Use oldItemStack.isSimilar(newItemStack) to test
	// whether the item has changed.
	// Note: This is performing much better compared to serializing and deserializing a YAML config
	// containing the item.
	public static @Nullable ItemStack convertItem(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return null;
		ItemMeta itemMeta = itemStack.getItemMeta(); // Can be null
		// Can be null:
		Map<? extends @NonNull String, @NonNull ?> serializedItemMeta = ItemSerialization.serializeItemMeta(itemMeta);
		if (serializedItemMeta == null) {
			// Item has no ItemMeta that could get converted:
			return itemStack;
		}
		// Can be null:
		ItemMeta deserializedItemMeta = ItemSerialization.deserializeItemMeta(serializedItemMeta);
		// TODO Avoid copy (also copies the metadata again) by serializing and deserializing the
		// complete ItemStack?
		ItemStack convertedItemStack = itemStack.clone();
		convertedItemStack.setItemMeta(deserializedItemMeta);
		return convertedItemStack;
	}

	public static int convertItems(
			@ReadOnly @Nullable ItemStack @ReadWrite [] contents,
			Predicate<@ReadOnly ? super @NonNull ItemStack> filter
	) {
		Validate.notNull(contents, "contents is null");
		Validate.notNull(filter, "filter is null");
		int convertedStacks = 0;
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack slotItem = contents[slot];
			if (ItemUtils.isEmpty(slotItem)) continue;
			slotItem = Unsafe.assertNonNull(slotItem);
			if (!filter.test(slotItem)) continue;

			ItemStack convertedItem = convertItem(slotItem);
			if (!slotItem.isSimilar(convertedItem)) {
				contents[slot] = convertedItem;
				convertedStacks += 1;
			}
		}
		return convertedStacks;
	}

	public static int convertItems(
			Inventory inventory,
			Predicate<@ReadOnly ? super @NonNull ItemStack> filter,
			boolean updateViewers
	) {
		Validate.notNull(inventory, "inventory is null");
		Validate.notNull(filter, "filter is null");

		// Convert inventory contents (includes armor and off hand slots for player inventories):
		@Nullable ItemStack[] contents = Unsafe.castNonNull(inventory.getContents());
		int convertedStacks = convertItems(contents, filter);
		if (convertedStacks > 0) {
			// Apply changes back to the inventory:
			InventoryUtils.setContents(inventory, contents);
		}

		if (inventory instanceof PlayerInventory) {
			// Also convert the item on the cursor:
			Player player = Unsafe.castNonNull(((PlayerInventory) inventory).getHolder());
			ItemStack cursor = player.getItemOnCursor();
			if (!ItemUtils.isEmpty(cursor) && filter.test(cursor)) {
				ItemStack convertedCursor = convertItem(cursor);
				if (!cursor.isSimilar(convertedCursor)) {
					convertedStacks += 1;
				}
			}
		}

		if (convertedStacks > 0 && updateViewers) {
			// Update inventory owner and viewers:
			InventoryUtils.updateInventoryLater(inventory);
		}
		return convertedStacks;
	}

	private ItemConversion() {
	}
}
