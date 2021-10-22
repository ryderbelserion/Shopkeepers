package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Utility functions related to loading and saving Bukkit, Minecraft, and plugin related objects from and to
 * {@link DataContainer}s.
 */
public class DataUtils {

	private DataUtils() {
	}

	public static Material loadMaterial(DataContainer dataContainer, String key) {
		String materialName = dataContainer.getString(key);
		if (materialName == null) return null;
		Material material = ItemUtils.parseMaterial(materialName); // Can be null
		if (material != null && material.isLegacy()) {
			return null;
		}
		return material;
	}

	// Additional processing whenever we save an item stack.
	// itemStack can be null.
	public static Object serializeItemStack(UnmodifiableItemStack itemStack) {
		// Shallow copy: Prevents SnakeYaml from representing the item stack using anchors and aliases if the same item
		// stack instance is saved to the same Yaml document multiple times in different contexts.
		return ItemUtils.shallowCopy(itemStack);
	}

	public static void saveItemStack(DataContainer dataContainer, String key, UnmodifiableItemStack itemStack) {
		dataContainer.set(key, serializeItemStack(itemStack));
	}

	// Additional processing whenever we load deserialized item stacks.
	public static ItemStack deserializeItemStack(ItemStack loadedItemStack) {
		// Note: Spigot creates Bukkit ItemStacks, whereas Paper automatically replaces the deserialized Bukkit
		// ItemStacks with CraftItemStacks. However, as long as the deserialized item stack is not compared directly to
		// an unmodifiable item stack (at least not without first being wrapped into an unmodifiable item stack itself),
		// and assuming that there are no inconsistencies in how CraftItemStacks and Bukkit ItemStacks are compared with
		// each other, this difference should not be relevant to us.

		// TODO SPIGOT-6716, PAPER-6437: The order of stored enchantments of enchanted books is not consistent. On
		// Paper, where the deserialized ItemStacks end up being CraftItemStacks, this difference in enchantment order
		// can cause issues when these deserialized item stacks are compared to other CraftItemStacks. Converting these
		// deserialized CraftItemStacks back to Bukkit ItemStacks ensures that the comparisons with other
		// CraftItemStacks ignore the enchantment order.
		if (loadedItemStack != null && loadedItemStack.getType() == Material.ENCHANTED_BOOK) {
			loadedItemStack = ItemUtils.ensureBukkitItemStack(loadedItemStack);
		}
		return loadedItemStack;
	}

	public static ItemStack loadItemStack(DataContainer dataContainer, String key) {
		ItemStack itemStack = dataContainer.getOfType(key, ItemStack.class);
		return deserializeItemStack(itemStack);
	}

	public static UnmodifiableItemStack loadUnmodifiableItemStack(DataContainer dataContainer, String key) {
		return UnmodifiableItemStack.of(loadItemStack(dataContainer, key));
	}
}
