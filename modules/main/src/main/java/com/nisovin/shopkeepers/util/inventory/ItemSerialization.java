package com.nisovin.shopkeepers.util.inventory;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.util.annotations.ReadOnly;

public class ItemSerialization {

	private static final String ITEM_META_SERIALIZATION_KEY = "ItemMeta";

	private ItemSerialization() {
	}

	public static Map<String, Object> serializeItemMeta(@ReadOnly ItemMeta itemMeta) {
		// Check if ItemMeta is empty (equivalent to ItemStack#hasItemMeta):
		if (!Bukkit.getItemFactory().equals(itemMeta, null)) {
			return itemMeta.serialize(); // Assert: Not null or empty
		} else {
			return null;
		}
	}

	public static ItemMeta deserializeItemMeta(@ReadOnly Map<String, @ReadOnly Object> itemMetaData) {
		if (itemMetaData == null) return null;
		// Get the class CraftBukkit internally uses for the deserialization:
		Class<? extends ConfigurationSerializable> serializableItemMetaClass = ConfigurationSerialization.getClassByAlias(ITEM_META_SERIALIZATION_KEY);
		if (serializableItemMetaClass == null) {
			throw new IllegalStateException("Missing ItemMeta ConfigurationSerializable class for key/alias '" + ITEM_META_SERIALIZATION_KEY + "'!");
		}
		// Can be null:
		ItemMeta itemMeta = (ItemMeta) ConfigurationSerialization.deserializeObject(itemMetaData, serializableItemMetaClass);
		return itemMeta;
	}
}
