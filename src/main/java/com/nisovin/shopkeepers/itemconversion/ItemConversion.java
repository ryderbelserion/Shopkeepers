package com.nisovin.shopkeepers.itemconversion;

import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.util.InventoryUtils;
import com.nisovin.shopkeepers.util.ItemSerialization;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PredicateUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;

public class ItemConversion {

	private ItemConversion() {
	}

	// Converts the given ItemStack to conform to Spigot's internal data format by running it through Spigot's item
	// de/serialization. Use oldItemStack.isSimilar(newItemStack) to test whether the item has changed.
	// Note: This is performing much better compared to serializing and deserializing a YAML config containing the item.
	public static ItemStack convertItem(@ReadOnly ItemStack itemStack) {
		if (itemStack == null) return null;
		ItemMeta itemMeta = itemStack.getItemMeta(); // Can be null
		Map<String, Object> serializedItemMeta = ItemSerialization.serializeItemMeta(itemMeta); // Can be null
		if (serializedItemMeta == null) {
			// Item has no ItemMeta that could get converted:
			return itemStack;
		}
		ItemMeta deserializedItemMeta = ItemSerialization.deserializeItemMeta(serializedItemMeta); // Can be null
		// TODO Avoid copy (also copies the metadata again) by serializing and deserializing the complete ItemStack?
		ItemStack convertedItemStack = itemStack.clone();
		convertedItemStack.setItemMeta(deserializedItemMeta);
		return convertedItemStack;
	}

	public static int convertItems(@ReadWrite ItemStack @ReadOnly [] contents, Predicate<@ReadOnly ItemStack> filter) {
		Validate.notNull(contents, "contents is null");
		filter = PredicateUtils.orAlwaysTrue(filter);
		int convertedStacks = 0;
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack slotItem = contents[slot];
			if (ItemUtils.isEmpty(slotItem)) continue;
			if (!filter.test(slotItem)) continue;
			ItemStack convertedItem = convertItem(slotItem);
			if (!slotItem.isSimilar(convertedItem)) {
				contents[slot] = convertedItem;
				convertedStacks += 1;
			}
		}
		return convertedStacks;
	}

	public static int convertItems(Inventory inventory, Predicate<@ReadOnly ItemStack> filter, boolean updateViewers) {
		Validate.notNull(inventory, "inventory is null");
		filter = PredicateUtils.orAlwaysTrue(filter);

		// Convert inventory contents (includes armor and off hand slots for player inventories):
		ItemStack[] contents = inventory.getContents();
		int convertedStacks = convertItems(contents, filter);
		if (convertedStacks > 0) {
			// Apply changes back to the inventory:
			InventoryUtils.setContents(inventory, contents);
		}

		if (inventory instanceof PlayerInventory) {
			// Also convert the item on the cursor:
			Player player = (Player) ((PlayerInventory) inventory).getHolder();
			ItemStack cursor = player.getItemOnCursor();
			if (!ItemUtils.isEmpty(cursor) && filter.test(cursor)) {
				ItemStack convertedCursor = convertItem(cursor);
				if (!cursor.isSimilar(convertedCursor)) {
					convertedStacks += 1;
				}
			}
		}

		if (convertedStacks > 0 && updateViewers) {
			// Update inventory viewers and owner:
			if (updateViewers) {
				InventoryUtils.updateInventoryLater(inventory);
			}
		}
		return convertedStacks;
	}
}
