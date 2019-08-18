package com.nisovin.shopkeepers.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * An unmodifiable object which stores type and meta data information of an item.
 */
public class ItemData implements Cloneable {

	private static final Consumer<String> SILENT_WARNING_HANDLER = (warning) -> {
	};
	private static final String ITEM_META_SERIALIZATION_KEY = "ItemMeta";
	private static final String META_TYPE_KEY = "meta-type";
	private static final String DISPLAY_NAME_KEY = "display-name";
	private static final String LORE_KEY = "lore";

	// special case: omitting 'blockMaterial' for empty TILE_ENTITY item meta
	private static final String TILE_ENTITY_BLOCK_MATERIAL_KEY = "blockMaterial";

	public static ItemData deserialize(Object dataObject) {
		return deserialize(dataObject, null);
	}

	public static ItemData deserialize(Object dataObject, Consumer<String> warningHandler) {
		if (warningHandler == null) {
			warningHandler = SILENT_WARNING_HANDLER; // ignore all warnings
		}
		if (dataObject == null) return null;

		String typeName = null;
		Map<String, Object> dataMap = null;
		if (dataObject instanceof String) {
			// load from compact representation (no additional item data):
			typeName = (String) dataObject;
			assert typeName != null;
		} else {
			if (dataObject instanceof ConfigurationSection) {
				dataMap = ((ConfigurationSection) dataObject).getValues(false); // returns a (shallow) copy
			} else if (dataObject instanceof Map) {
				// make a (shallow) copy of the map, since we will later insert missing data and don't want to modify
				// the original data from the config:
				dataMap = new LinkedHashMap<>();
				for (Entry<?, ?> entry : ((Map<?, ?>) dataObject).entrySet()) {
					dataMap.put(entry.getKey().toString(), entry.getValue());
				}
			} else {
				warningHandler.accept("Unknown item data: " + dataObject);
				return null;
			}
			assert dataMap != null; // assert: dataMap is a (shallow) copy

			Object typeData = dataMap.get("type");
			if (typeData != null) {
				typeName = typeData.toString();
			}
			if (typeName == null) {
				// missing item type information:
				warningHandler.accept("Missing item type");
				return null;
			}
			assert typeName != null;

			// skip meta data loading if no further data (besides item type) is given:
			if (dataMap.size() <= 1) {
				dataMap = null;
			}
		}
		assert typeName != null;

		// assuming up-to-date material name (performs no conversions besides basic formatting):
		Material type = Material.matchMaterial(typeName);
		if (type == null) {
			// unknown item type:
			warningHandler.accept("Unknown item type: " + typeName);
			return null;
		}

		// create item stack (still misses meta data):
		ItemStack dataItem = new ItemStack(type);

		// load additional meta data:
		if (dataMap != null) {
			// prepare for meta data deserialization (assumes dataMap is modifiable):
			// note: additional information does not need to be removed, but simply gets ignored (eg. item type)

			// recursively replace all config sections with maps, since ItemMeta deserialization expects Maps:
			ConfigUtils.convertSectionsToMaps(dataMap);

			// determine meta type by creating the serialization of a dummy item meta:
			ItemMeta dummyItemMeta = dataItem.getItemMeta();
			dummyItemMeta.setDisplayName("dummy name"); // ensure item meta is not empty
			Object metaType = dummyItemMeta.serialize().get(META_TYPE_KEY);
			if (metaType == null) {
				throw new IllegalStateException("Couldn't determine meta type with key '" + META_TYPE_KEY + "'!");
			}
			// insert meta type:
			dataMap.put(META_TYPE_KEY, metaType);

			// convert color codes for display name and lore:
			Object displayNameData = dataMap.get(DISPLAY_NAME_KEY);
			if (displayNameData instanceof String) { // also checks for null
				dataMap.put(DISPLAY_NAME_KEY, Utils.colorize((String) displayNameData));
			}
			Object loreData = dataMap.get(LORE_KEY);
			if (loreData instanceof List) { // also checks for null
				dataMap.put(LORE_KEY, Utils.colorizeUnknown((List<?>) loreData));
			}

			// deserialize item meta:
			// get the class CraftBukkit internally uses for the deserialization:
			Class<? extends ConfigurationSerializable> serializableItemMetaClass = ConfigurationSerialization.getClassByAlias(ITEM_META_SERIALIZATION_KEY);
			if (serializableItemMetaClass == null) {
				throw new IllegalStateException("Missing ItemMeta ConfigurationSerializable class for key/alias '" + ITEM_META_SERIALIZATION_KEY + "'!");
			}
			// can be null:
			ItemMeta itemMeta = (ItemMeta) ConfigurationSerialization.deserializeObject(dataMap, serializableItemMetaClass);

			// apply item meta:
			dataItem.setItemMeta(itemMeta);
		}

		// create ItemData:
		ItemData itemData = new ItemData(dataItem);
		return itemData;
	}

	/////

	private final ItemStack dataItem;
	// cache serialized item meta data, to avoid doing it again for every comparison:
	private Map<String, Object> serializedData = null; // gets lazily initialized (only when actually needed)

	public ItemData(Material type) {
		this(new ItemStack(type));
	}

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

	// not null
	private Map<String, Object> getSerializedData() {
		// lazily cache the serialized data:
		if (serializedData == null) {
			ItemMeta itemMeta = dataItem.getItemMeta();
			// check whether itemMeta is empty; equivalent to ItemStack#hasItemMeta
			if (itemMeta != null && !Bukkit.getItemFactory().equals(itemMeta, null)) {
				serializedData = itemMeta.serialize(); // assert: not null nor empty
			} else {
				serializedData = Collections.emptyMap(); // ensure field is not null after initialization
			}
		}
		assert serializedData != null;
		return serializedData;
	}

	public boolean hasItemMeta() {
		return !this.getSerializedData().isEmpty(); // equivalent to dataItem.hasItemMeta()
	}

	public ItemMeta getItemMeta() {
		// returns a copy, therefore cannot modify the original data:
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
		return this.matches(item, false); // not matching partial lists
	}

	public boolean matches(ItemStack item, boolean matchPartialLists) {
		// same type and matching data:
		return ItemUtils.matchesData(item, this.getType(), this.getSerializedData(), matchPartialLists);
	}

	public boolean matches(ItemData itemData) {
		return this.matches(itemData, false); // not matching partial lists
	}

	// given ItemData is of same type and has data matching this ItemData
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
		return new ItemData(dataItem); // clones the item internally
	}

	public Object serialize() {
		Map<String, Object> serializedData = this.getSerializedData();
		if (serializedData.isEmpty()) {
			// use a more compact representation if there is no additional item data:
			return dataItem.getType().name();
		}

		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put("type", dataItem.getType().name());

		for (Entry<String, Object> entry : serializedData.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// omitting any data which can be easily restored during deserialization:
			// omit meta type key:
			if (META_TYPE_KEY.equals(key)) continue;

			// omit 'blockMaterial' for empty TILE_ENTITY item meta:
			if (TILE_ENTITY_BLOCK_MATERIAL_KEY.equals(key)) {
				// check if specific meta type only contains unspecific meta data:
				ItemMeta specificItemMeta = dataItem.getItemMeta();
				// TODO relies on some material with unspecific item meta
				ItemMeta unspecificItemMeta = Bukkit.getItemFactory().asMetaFor(specificItemMeta, Material.STONE);
				if (Bukkit.getItemFactory().equals(unspecificItemMeta, specificItemMeta)) {
					continue; // skip 'blockMaterial' entry
				}
			}

			// use alternative color codes for display name and lore:
			if (DISPLAY_NAME_KEY.equals(key)) {
				if (value instanceof String) {
					value = Utils.decolorize((String) value);
				}
			} else if (LORE_KEY.equals(key)) {
				if (value instanceof List) {
					value = Utils.decolorizeUnknown((List<?>) value);
				}
			}

			// move into data map: avoiding a deep copy, since it is assumed to not be needed
			dataMap.put(key, value);
		}
		return dataMap;
	}
}
