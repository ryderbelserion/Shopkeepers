package com.nisovin.shopkeepers.util.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.bukkit.MaterialValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.MinecraftEnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An immutable object that stores item type and meta data information.
 */
public final class ItemData {

	private static final Property<Material> ITEM_TYPE = new BasicProperty<Material>()
			.dataKeyAccessor("type", MinecraftEnumSerializers.Materials.LENIENT)
			.validator(MaterialValidators.IS_ITEM)
			.validator(MaterialValidators.NON_LEGACY)
			.build();

	private static final String META_TYPE_KEY = "meta-type";
	private static final String DISPLAY_NAME_KEY = "display-name";
	private static final String LORE_KEY = "lore";

	// Special case: Omitting 'blockMaterial' for empty TILE_ENTITY item meta.
	private static final String TILE_ENTITY_BLOCK_MATERIAL_KEY = "blockMaterial";

	// Entries are lazily added and then cached:
	// The mapped value can be null for items that do not support item meta.
	private static final Map<Material, String> META_TYPE_BY_ITEM_TYPE = new HashMap<>();

	// Returns null for items that do not support ItemMeta.
	private static String getMetaType(Material itemType) {
		Validate.notNull(itemType, "itemType is null");
		// Check the cache:
		String metaType = META_TYPE_BY_ITEM_TYPE.get(itemType);
		if (metaType != null) {
			return metaType;
		}
		assert metaType == null;
		if (META_TYPE_BY_ITEM_TYPE.containsKey(itemType)) {
			// Item type is mapped to null. -> Item does not support item meta.
			return null;
		}

		// Determine the meta type from the item's serialized ItemMeta:
		ItemMeta itemMeta = new ItemStack(itemType).getItemMeta(); // Can be null
		if (itemMeta != null) {
			metaType = (String) itemMeta.serialize().get(META_TYPE_KEY);
			if (metaType == null) {
				throw new IllegalStateException("Could not determine the meta type of "
						+ itemMeta.getClass().getName() + "!");
			}
		} // Else: Item does not support meta data. metaType remains null.

		// Cache the meta type (can be null if the item does not support ItemMeta):
		META_TYPE_BY_ITEM_TYPE.put(itemType, metaType);

		return metaType; // Can be null
	}

	/**
	 * A {@link DataSerializer} for values of type {@link ItemData}.
	 */
	public static final DataSerializer<ItemData> SERIALIZER = new DataSerializer<ItemData>() {
		@Override
		public Object serialize(ItemData value) {
			Validate.notNull(value, "value is null");
			Map<String, Object> serializedMetaData = value.getSerializedMetaData();
			if (serializedMetaData.isEmpty()) {
				// Use a more compact representation if there is no additional item data:
				return value.dataItem.getType().name();
			}

			DataContainer itemDataData = DataContainer.create();
			itemDataData.set(ITEM_TYPE, value.dataItem.getType());

			for (Entry<String, Object> metaEntry : serializedMetaData.entrySet()) {
				String metaKey = metaEntry.getKey();
				Object metaValue = metaEntry.getValue();

				// We omit any data that can be easily restored during deserialization:
				// Omit meta type key:
				if (META_TYPE_KEY.equals(metaKey)) continue;

				// Omit 'blockMaterial' for empty TILE_ENTITY item meta:
				if (TILE_ENTITY_BLOCK_MATERIAL_KEY.equals(metaKey)) {
					// Check if specific meta type only contains unspecific meta data:
					ItemMeta specificItemMeta = value.dataItem.getItemMeta();
					// TODO Relies on some material with unspecific item meta.
					ItemMeta unspecificItemMeta = Bukkit.getItemFactory().asMetaFor(specificItemMeta, Material.STONE);
					if (Bukkit.getItemFactory().equals(unspecificItemMeta, specificItemMeta)) {
						continue; // Skip 'blockMaterial' entry
					}
				}

				// Use alternative color codes for display name and lore:
				if (DISPLAY_NAME_KEY.equals(metaKey)) {
					if (metaValue instanceof String) {
						metaValue = TextUtils.decolorize((String) metaValue);
					}
				} else if (LORE_KEY.equals(metaKey)) {
					if (metaValue instanceof List) {
						metaValue = TextUtils.decolorizeUnknown((List<?>) metaValue);
					}
				}

				// Insert the entry into the data container:
				// A deep copy is assumed to not be needed.
				itemDataData.set(metaKey, metaValue);
			}
			return itemDataData.serialize();
		}

		@Override
		public ItemData deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			Material itemType;
			DataContainer itemDataData = null;
			if (data instanceof String) {
				// Reconstruct from compact representation (no additional item meta data):
				itemType = MinecraftEnumSerializers.Materials.LENIENT.deserialize((String) data);
				try {
					ITEM_TYPE.validateValue(itemType);
				} catch (Exception e) {
					throw new InvalidDataException(e.getMessage(), e);
				}
			} else {
				itemDataData = DataContainerSerializers.DEFAULT.deserialize(data);
				try {
					itemType = itemDataData.get(ITEM_TYPE);
				} catch (MissingDataException e) {
					throw new InvalidDataException(e.getMessage(), e);
				}

				// Skip the meta data loading if no further data (besides the item type) is given:
				if (itemDataData.size() <= 1) {
					itemDataData = null;
				}
			}
			assert itemType != null;

			// Create item stack (still misses meta data):
			ItemStack dataItem = new ItemStack(itemType);

			// Load additional meta data:
			if (itemDataData != null) {
				// Prepare the data for the meta data deserialization:
				// We (shallow) copy the data to a new Map, because we will have to insert additional data for the
				// ItemMeta to be deserializable, and don't want to modify the given original data.
				// Note: Additional information (eg. the item type) does not need to be removed, but is simply ignored.
				Map<String, Object> itemMetaData = itemDataData.getValuesCopy();

				// Recursively replace all config sections with Maps, because the ItemMeta deserialization expects Maps:
				ConfigUtils.convertSectionsToMaps(itemMetaData);

				// Determine the meta type:
				String metaType = getMetaType(itemType);
				if (metaType == null) {
					throw new InvalidDataException("Items of type " + itemType.name() + " do not support meta data!");
				}

				// Insert meta type:
				itemMetaData.put(META_TYPE_KEY, metaType);

				// Convert color codes for display name and lore:
				Object displayNameData = itemMetaData.get(DISPLAY_NAME_KEY);
				if (displayNameData instanceof String) { // Also checks for null
					itemMetaData.put(DISPLAY_NAME_KEY, TextUtils.colorize((String) displayNameData));
				}
				List<?> loreData = itemDataData.getList(LORE_KEY); // Null if the data is not a list
				if (loreData != null) {
					itemMetaData.put(LORE_KEY, TextUtils.colorizeUnknown(loreData));
				}

				// Deserialize the ItemMeta:
				ItemMeta itemMeta = ItemSerialization.deserializeItemMeta(itemMetaData); // Can be null

				// Apply the ItemMeta:
				dataItem.setItemMeta(itemMeta);
			}

			// Create ItemData:
			// Unmodifiable wrapper: Avoids creating another item copy during construction.
			ItemData itemData = new ItemData(UnmodifiableItemStack.of(dataItem));
			return itemData;
		}
	};

	/////

	private final UnmodifiableItemStack dataItem; // Has amount of 1
	// Cache serialized item meta data, to avoid serializing it again for every comparison:
	private @ReadOnly Map<String, @ReadOnly Object> serializedMetaData = null; // Gets lazily initialized when needed

	public ItemData(Material type) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.of(new ItemStack(type)));
	}

	// The display name and lore are expected to use Minecraft's color codes.
	public ItemData(Material type, String displayName, @ReadOnly List<String> lore) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.of(ItemUtils.createItemStack(type, 1, displayName, lore)));
	}

	public ItemData(ItemData otherItemData, String displayName, @ReadOnly List<String> lore) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.of(ItemUtils.createItemStack(otherItemData, 1, displayName, lore)));
	}

	/**
	 * Creates a new {@link ItemData} with the data of the given item stack.
	 * <p>
	 * If the given item stack is an {@link UnmodifiableItemStack}, it is assumed to be immutable and the
	 * {@link ItemData} is allowed store it without making a copy of it first.
	 * 
	 * @param dataItem
	 *            the data item, not <code>null</code>
	 */
	public ItemData(@ReadOnly ItemStack dataItem) {
		this(ItemUtils.unmodifiableCopyWithAmount(dataItem, 1));
	}

	// dataItem is assumed to be immutable.
	public ItemData(UnmodifiableItemStack dataItem) {
		Validate.notNull(dataItem, "dataItem is null");
		this.dataItem = ItemUtils.unmodifiableCopyWithAmount(dataItem.asItemStack(), 1);
	}

	public Material getType() {
		return dataItem.getType();
	}

	// Creates a copy of this ItemData, but changes the item type. If the new type matches the previous type, the
	// current ItemData is returned.
	// Any incompatible meta data is removed.
	public ItemData withType(Material type) {
		Validate.notNull(type, "type is null");
		Validate.isTrue(type.isItem(), () -> "type is not an item: " + type);
		if (this.getType() == type) return this;
		ItemStack newDataItem = this.createItemStack();
		newDataItem.setType(type);
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		return new ItemData(UnmodifiableItemStack.of(newDataItem));
	}

	// Not null.
	private Map<String, Object> getSerializedMetaData() {
		// Lazily cache the serialized data:
		if (serializedMetaData == null) {
			ItemMeta itemMeta = dataItem.getItemMeta();
			serializedMetaData = ItemSerialization.serializeItemMeta(itemMeta);
			if (serializedMetaData == null) {
				// Ensure that the field is not null after initialization:
				serializedMetaData = Collections.emptyMap();
			}
		}
		assert serializedMetaData != null;
		return serializedMetaData;
	}

	public boolean hasItemMeta() {
		return !this.getSerializedMetaData().isEmpty(); // Equivalent to dataItem.hasItemMeta()
	}

	public ItemMeta getItemMeta() {
		// Returns a copy, therefore cannot modify the original data:
		return dataItem.getItemMeta();
	}

	// Creates an item stack with an amount of 1.
	public ItemStack createItemStack() {
		return this.createItemStack(1);
	}

	public ItemStack createItemStack(int amount) {
		return ItemUtils.copyWithAmount(dataItem, amount);
	}

	public boolean isSimilar(@ReadOnly ItemStack other) {
		return dataItem.isSimilar(other);
	}

	public boolean isSimilar(UnmodifiableItemStack other) {
		return other != null && other.isSimilar(dataItem);
	}

	public boolean matches(@ReadOnly ItemStack item) {
		return this.matches(item, false); // Not matching partial lists
	}

	public boolean matches(UnmodifiableItemStack item) {
		return this.matches(ItemUtils.asItemStackOrNull(item));
	}

	public boolean matches(@ReadOnly ItemStack item, boolean matchPartialLists) {
		// Same type and matching data:
		return ItemUtils.matchesData(item, this.getType(), this.getSerializedMetaData(), matchPartialLists);
	}

	public boolean matches(UnmodifiableItemStack item, boolean matchPartialLists) {
		return this.matches(ItemUtils.asItemStackOrNull(item), matchPartialLists);
	}

	public boolean matches(ItemData itemData) {
		return this.matches(itemData, false); // Not matching partial lists
	}

	// Given ItemData is of same type and has data matching this ItemData.
	public boolean matches(ItemData itemData, boolean matchPartialLists) {
		if (itemData == null) return false;
		if (itemData.getType() != this.getType()) return false;
		return ItemUtils.matchesData(itemData.getSerializedMetaData(), this.getSerializedMetaData(), matchPartialLists);
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

	public Object serialize() {
		return SERIALIZER.serialize(this);
	}
}
