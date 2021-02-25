package com.nisovin.shopkeepers.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * An unmodifiable object which stores type and meta data information of an item.
 */
public class ItemData implements Cloneable {

	public static class ItemDataDeserializeException extends Exception {

		private static final long serialVersionUID = -6637983932875623362L;

		public ItemDataDeserializeException(String message) {
			super(message);
		}

		public ItemDataDeserializeException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class UnknownItemTypeException extends ItemDataDeserializeException {

		private static final long serialVersionUID = -6123823171023440870L;

		public UnknownItemTypeException(String message) {
			super(message);
		}

		public UnknownItemTypeException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static final String META_TYPE_KEY = "meta-type";
	private static final String DISPLAY_NAME_KEY = "display-name";
	private static final String LORE_KEY = "lore";

	// Special case: Omitting 'blockMaterial' for empty TILE_ENTITY item meta.
	private static final String TILE_ENTITY_BLOCK_MATERIAL_KEY = "blockMaterial";

	// Only returns null if the input data is null.
	public static ItemData deserialize(Object dataObject) throws ItemDataDeserializeException {
		if (dataObject == null) return null;

		String typeName = null;
		Map<String, Object> dataMap = null;
		if (dataObject instanceof String) {
			// Load from compact representation (no additional item data):
			typeName = (String) dataObject;
			assert typeName != null;
		} else {
			if (dataObject instanceof ConfigurationSection) {
				dataMap = ((ConfigurationSection) dataObject).getValues(false); // Returns a (shallow) copy
			} else if (dataObject instanceof Map) {
				// Make a (shallow) copy of the map, since we will later insert missing data and don't want to modify
				// the original data from the config:
				dataMap = new LinkedHashMap<>();
				for (Entry<?, ?> entry : ((Map<?, ?>) dataObject).entrySet()) {
					dataMap.put(entry.getKey().toString(), entry.getValue());
				}
			} else {
				throw new ItemDataDeserializeException("Unknown item data representation: " + dataObject);
			}
			assert dataMap != null; // Assert: dataMap is a (shallow) copy

			Object typeData = dataMap.get("type");
			if (typeData != null) {
				typeName = typeData.toString();
			}
			if (typeName == null) {
				// Missing item type information:
				throw new ItemDataDeserializeException("Missing item type");
			}
			assert typeName != null;

			// Skip meta data loading if no further data (besides item type) is given:
			if (dataMap.size() <= 1) {
				dataMap = null;
			}
		}
		assert typeName != null;

		// Assuming up-to-date material name (performs no conversions besides basic formatting):
		Material type = Material.matchMaterial(typeName);
		if (type == null) {
			// Unknown item type:
			throw new UnknownItemTypeException("Unknown item type: " + typeName);
		}

		// Create item stack (still misses meta data):
		ItemStack dataItem = new ItemStack(type);

		// Load additional meta data:
		if (dataMap != null) {
			// Prepare for meta data deserialization (assumes dataMap is modifiable):
			// Note: Additional information does not need to be removed, but simply gets ignored (eg. item type).

			// Recursively replace all config sections with maps, since ItemMeta deserialization expects Maps:
			ConfigUtils.convertSectionsToMaps(dataMap);

			// Determine meta type by creating the serialization of a dummy item meta:
			ItemMeta dummyItemMeta = dataItem.getItemMeta();
			dummyItemMeta.setDisplayName("dummy name"); // Ensure item meta is not empty
			Object metaType = dummyItemMeta.serialize().get(META_TYPE_KEY);
			if (metaType == null) {
				throw new IllegalStateException("Could not determine meta type with key '" + META_TYPE_KEY + "'!");
			}
			// Insert meta type:
			dataMap.put(META_TYPE_KEY, metaType);

			// Convert color codes for display name and lore:
			Object displayNameData = dataMap.get(DISPLAY_NAME_KEY);
			if (displayNameData instanceof String) { // Also checks for null
				dataMap.put(DISPLAY_NAME_KEY, TextUtils.colorize((String) displayNameData));
			}
			Object loreData = dataMap.get(LORE_KEY);
			if (loreData instanceof List) { // Also checks for null
				dataMap.put(LORE_KEY, TextUtils.colorizeUnknown((List<?>) loreData));
			}

			// Deserialize ItemMeta:
			ItemMeta itemMeta = ItemUtils.deserializeItemMeta(dataMap); // Can be null

			// Apply ItemMeta:
			dataItem.setItemMeta(itemMeta);
		}

		// Create ItemData:
		ItemData itemData = new ItemData(dataItem);
		return itemData;
	}

	/////

	private final ItemStack dataItem;
	// Cache serialized item meta data, to avoid doing it again for every comparison:
	private Map<String, Object> serializedData = null; // Gets lazily initialized (only when actually needed)

	public ItemData(Material type) {
		this(new ItemStack(type));
	}

	// The display name and lore are expected to use Minecraft's color codes.
	public ItemData(Material type, String displayName, List<String> lore) {
		this(ItemUtils.createItemStack(type, 1, displayName, lore));
	}

	public ItemData(ItemStack dataItem) {
		Validate.notNull(dataItem, "The given data ItemStack is null!");
		this.dataItem = dataItem.clone();
		this.dataItem.setAmount(1);
	}

	public Material getType() {
		return dataItem.getType();
	}

	// Creates a copy of this ItemData, but changes the item type.
	// Any incompatible data gets removed.
	public ItemData withType(Material type) {
		ItemStack newDataItem = this.createItemStack();
		newDataItem.setType(type);
		return new ItemData(newDataItem);
	}

	// Not null.
	private Map<String, Object> getSerializedData() {
		// Lazily cache the serialized data:
		if (serializedData == null) {
			ItemMeta itemMeta = dataItem.getItemMeta();
			serializedData = ItemUtils.serializeItemMeta(itemMeta);
			if (serializedData == null) {
				// Ensure that the field is not null after initialization:
				serializedData = Collections.emptyMap();
			}
		}
		assert serializedData != null;
		return serializedData;
	}

	public boolean hasItemMeta() {
		return !this.getSerializedData().isEmpty(); // Equivalent to dataItem.hasItemMeta()
	}

	public ItemMeta getItemMeta() {
		// Returns a copy, therefore cannot modify the original data:
		return dataItem.getItemMeta();
	}

	// amount of 1
	public ItemStack createItemStack() {
		return dataItem.clone();
	}

	public ItemStack createItemStack(int amount) {
		ItemStack item = this.createItemStack();
		item.setAmount(amount);
		return item;
	}

	public boolean isSimilar(ItemStack other) {
		return dataItem.isSimilar(other);
	}

	public boolean matches(ItemStack item) {
		return this.matches(item, false); // Not matching partial lists
	}

	public boolean matches(ItemStack item, boolean matchPartialLists) {
		// Same type and matching data:
		return ItemUtils.matchesData(item, this.getType(), this.getSerializedData(), matchPartialLists);
	}

	public boolean matches(ItemData itemData) {
		return this.matches(itemData, false); // Not matching partial lists
	}

	// Given ItemData is of same type and has data matching this ItemData.
	public boolean matches(ItemData itemData, boolean matchPartialLists) {
		if (itemData == null) return false;
		if (itemData.getType() != this.getType()) return false;
		return ItemUtils.matchesData(itemData.getSerializedData(), this.getSerializedData(), matchPartialLists);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ItemData [data=");
		builder.append(dataItem);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dataItem.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ItemData)) return false;
		ItemData other = (ItemData) obj;
		if (!dataItem.equals(other.dataItem)) return false;
		return true;
	}

	@Override
	public ItemData clone() {
		return new ItemData(dataItem); // Clones the item internally
	}

	public Object serialize() {
		Map<String, Object> serializedData = this.getSerializedData();
		if (serializedData.isEmpty()) {
			// Use a more compact representation if there is no additional item data:
			return dataItem.getType().name();
		}

		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put("type", dataItem.getType().name());

		for (Entry<String, Object> entry : serializedData.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Omitting any data which can be easily restored during deserialization:
			// Omit meta type key:
			if (META_TYPE_KEY.equals(key)) continue;

			// Omit 'blockMaterial' for empty TILE_ENTITY item meta:
			if (TILE_ENTITY_BLOCK_MATERIAL_KEY.equals(key)) {
				// Check if specific meta type only contains unspecific meta data:
				ItemMeta specificItemMeta = dataItem.getItemMeta();
				// TODO Relies on some material with unspecific item meta.
				ItemMeta unspecificItemMeta = Bukkit.getItemFactory().asMetaFor(specificItemMeta, Material.STONE);
				if (Bukkit.getItemFactory().equals(unspecificItemMeta, specificItemMeta)) {
					continue; // Skip 'blockMaterial' entry
				}
			}

			// Use alternative color codes for display name and lore:
			if (DISPLAY_NAME_KEY.equals(key)) {
				if (value instanceof String) {
					value = TextUtils.decolorize((String) value);
				}
			} else if (LORE_KEY.equals(key)) {
				if (value instanceof List) {
					value = TextUtils.decolorizeUnknown((List<?>) value);
				}
			}

			// Move into data map: Avoiding a deep copy, since it is assumed to not be needed.
			dataMap.put(key, value);
		}
		return dataMap;
	}
}
