package com.nisovin.shopkeepers.util.inventory;

import java.util.Collections;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

public final class ItemSerialization {

	private static final String ITEM_META_SERIALIZATION_KEY = "ItemMeta";

	public static @Nullable Map<? extends String, @NonNull ?> serializeItemMeta(
			@ReadOnly @Nullable ItemMeta itemMeta
	) {
		// Check if ItemMeta is empty (equivalent to ItemStack#hasItemMeta):
		if (!Bukkit.getItemFactory().equals(itemMeta, null)) {
			assert itemMeta != null;
			return Unsafe.cast(itemMeta.serialize()); // Assert: Not null or empty
		} else {
			return null;
		}
	}

	public static Map<? extends String, @NonNull ?> serializeItemMetaOrEmpty(
			@ReadOnly @Nullable ItemMeta itemMeta
	) {
		Map<? extends String, @NonNull ?> serializedItemMeta = serializeItemMeta(itemMeta);
		if (serializedItemMeta != null) {
			return serializedItemMeta;
		} else {
			return Collections.<String, Object>emptyMap();
		}
	}

	public static @Nullable ItemMeta deserializeItemMeta(
			@ReadOnly @Nullable Map<? extends @Nullable String, @ReadOnly ?> itemMetaData
	) {
		if (itemMetaData == null) return null;

		// Get the class CraftBukkit internally uses for the deserialization:
		Class<? extends ConfigurationSerializable> serializableItemMetaClass
				= ConfigurationSerialization.getClassByAlias(ITEM_META_SERIALIZATION_KEY);
		if (serializableItemMetaClass == null) {
			throw new IllegalStateException(
					"Missing ItemMeta ConfigurationSerializable class for key/alias '"
							+ ITEM_META_SERIALIZATION_KEY + "'!"
			);
		}

		// Can be null:
		ItemMeta itemMeta = (ItemMeta) ConfigurationSerialization.deserializeObject(
				Unsafe.cast(itemMetaData),
				serializableItemMetaClass
		);
		return itemMeta;
	}

	private ItemSerialization() {
	}
}
