package com.nisovin.shopkeepers.util.inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.bukkit.MaterialValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.MinecraftEnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.java.Lazy;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An immutable object that stores item type and metadata information.
 */
public final class ItemData {

	/**
	 * Disabled by default because the conversion from the plain to the Json text format may be
	 * unstable from one server version to another.
	 */
	private static boolean SERIALIZER_PREFERS_PLAIN_TEXT_FORMAT = false;

	public static void serializerPrefersPlainTextFormat(boolean preferPlainTextFormat) {
		SERIALIZER_PREFERS_PLAIN_TEXT_FORMAT = preferPlainTextFormat;
	}

	public static void resetSerializerPrefersPlainTextFormat() {
		serializerPrefersPlainTextFormat(false);
	}

	private static final Property<@NonNull Material> ITEM_TYPE = new BasicProperty<@NonNull Material>()
			.dataKeyAccessor("type", MinecraftEnumSerializers.Materials.LENIENT)
			.validator(MaterialValidators.IS_ITEM)
			.validator(MaterialValidators.NON_LEGACY)
			.build();

	private static final String META_TYPE_KEY = "meta-type";
	private static final String DISPLAY_NAME_KEY = "display-name";
	private static final String LORE_KEY = "lore";
	private static final String LOC_NAME_KEY = "loc-name";

	// Special case: Omitting 'blockMaterial' for empty TILE_ENTITY item meta.
	private static final String TILE_ENTITY_BLOCK_MATERIAL_KEY = "blockMaterial";

	// Entries are lazily added and then cached:
	// The mapped value can be null for items that do not support item meta.
	private static final Map<@NonNull Material, @Nullable String> META_TYPE_BY_ITEM_TYPE = new HashMap<>();

	// Returns null for items that do not support ItemMeta.
	private static @Nullable String getMetaType(Material itemType) {
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
		} // Else: Item does not support metadata. metaType remains null.

		// Cache the meta type (can be null if the item does not support ItemMeta):
		META_TYPE_BY_ITEM_TYPE.put(itemType, metaType);

		return metaType; // Can be null
	}

	/**
	 * A {@link DataSerializer} for values of type {@link ItemData}.
	 */
	public static final DataSerializer<@NonNull ItemData> SERIALIZER = new DataSerializer<@NonNull ItemData>() {
		@Override
		public @Nullable Object serialize(ItemData value) {
			Validate.notNull(value, "value is null");
			Map<? extends @NonNull String, @NonNull ?> serializedMetaData = value.getSerializedMetaData();
			if (serializedMetaData.isEmpty()) {
				// Use a more compact representation if there is no additional item data:
				return value.getType().name();
			}

			DataContainer itemDataData = DataContainer.create();
			itemDataData.set(ITEM_TYPE, value.getType());

			// Lazily instantiated, only if needed during serialization:
			Lazy<@Nullable ItemMeta> lazyItemMeta = new Lazy<>(value::getItemMeta);
			Lazy<@NonNull Map<? extends @NonNull String, @NonNull ?>> lazyPlainSerializedMetaData = new Lazy<>(
					() -> {
						ItemMeta itemMeta = lazyItemMeta.get();
						if (itemMeta != null) {
							// ItemMeta#getDisplayName and #getLore convert from the ItemMeta's
							// internal Json representations to plain legacy text representations.
							// By applying those plain representations back to the ItemMeta and then
							// serializing the ItemMeta, we are able check if the resulting Json
							// representations in the newly serialized ItemMeta matches our original
							// serialized representations.
							if (itemMeta.hasDisplayName()) {
								itemMeta.setDisplayName(itemMeta.getDisplayName());
							}
							if (itemMeta.hasLore()) {
								itemMeta.setLore(itemMeta.getLore());
							}
							if (itemMeta.hasLocalizedName()) {
								itemMeta.setLocalizedName(itemMeta.getLocalizedName());
							}
						}
						return ItemSerialization.serializeItemMetaOrEmpty(itemMeta);
					}
			);
			boolean preferPlainTextFormat = SERIALIZER_PREFERS_PLAIN_TEXT_FORMAT;

			for (Entry<? extends @NonNull String, @NonNull ?> metaEntry : serializedMetaData.entrySet()) {
				String metaKey = metaEntry.getKey();
				Object metaValue = metaEntry.getValue();

				// We omit any data that can be easily restored during deserialization:
				// Omit meta type key:
				if (META_TYPE_KEY.equals(metaKey)) continue;

				// Omit 'blockMaterial' for empty TILE_ENTITY item meta:
				if (TILE_ENTITY_BLOCK_MATERIAL_KEY.equals(metaKey)) {
					// Check if specific meta type only contains unspecific metadata:
					ItemMeta specificItemMeta = Unsafe.assertNonNull(lazyItemMeta.get());
					// TODO Relies on some material with unspecific item meta.
					ItemMeta unspecificItemMeta = Bukkit.getItemFactory().asMetaFor(
							specificItemMeta,
							Material.STONE
					);
					if (Bukkit.getItemFactory().equals(unspecificItemMeta, specificItemMeta)) {
						continue; // Skip 'blockMaterial' entry
					}
				}

				// Special handling of text data:
				// - We might optionally prefer the plain text format with color codes for texts
				// that the server can loss-less convert back to the Json text format.
				// - We want to use alternative color codes.
				if (DISPLAY_NAME_KEY.equals(metaKey)) {
					if (metaValue instanceof String) {
						String serializedDisplayName = (String) metaValue;

						if (preferPlainTextFormat) {
							ItemMeta itemMeta = Unsafe.assertNonNull(lazyItemMeta.get());
							String plainDisplayName = itemMeta.getDisplayName();
							if (!serializedDisplayName.equals(plainDisplayName)) {
								// The serialized display name might be in Json format. Check if we
								// can preserve it even if we serialize it in plain format:
								String plainSerializedDisplayName = Unsafe.castNonNull(
										lazyPlainSerializedMetaData.get().get(DISPLAY_NAME_KEY)
								);
								if (serializedDisplayName.equals(plainSerializedDisplayName)) {
									// Use the plain representation:
									serializedDisplayName = plainDisplayName;
								}
							}
						}

						// Use alternative color codes:
						metaValue = TextUtils.decolorize(serializedDisplayName);
					}
				} else if (LORE_KEY.equals(metaKey)) {
					if (metaValue instanceof List) {
						List<?> serializedLore = (List<?>) metaValue;

						if (preferPlainTextFormat) {
							ItemMeta itemMeta = Unsafe.assertNonNull(lazyItemMeta.get());
							List<?> plainLore = Unsafe.assertNonNull(itemMeta.getLore());
							if (!serializedLore.equals(plainLore)) {
								// The serialized lore might be in Json format. Check if we can
								// preserve it even if we serialize it in plain format:
								List<?> plainSerializedLore = Unsafe.castNonNull(
										lazyPlainSerializedMetaData.get().get(LORE_KEY)
								);
								if (serializedLore.equals(plainSerializedLore)) {
									// Use the plain representation:
									serializedLore = plainLore;
								}
							}
						}

						// Use alternative color codes:
						metaValue = TextUtils.decolorizeUnknown(serializedLore);
					}
				} else if (LOC_NAME_KEY.equals(metaKey)) {
					if (metaValue instanceof String) {
						String serializedLocName = (String) metaValue;

						if (preferPlainTextFormat) {
							ItemMeta itemMeta = Unsafe.assertNonNull(lazyItemMeta.get());
							String plainLocName = itemMeta.getLocalizedName();
							if (!serializedLocName.equals(plainLocName)) {
								// The serialized localized name might be in Json format. Check if
								// we can preserve it even if we serialize it in plain format:
								String plainSerializedLocName = Unsafe.castNonNull(
										lazyPlainSerializedMetaData.get().get(LOC_NAME_KEY)
								);
								if (serializedLocName.equals(plainSerializedLocName)) {
									// Use the plain representation:
									serializedLocName = plainLocName;
								}
							}
						}

						// Use alternative color codes:
						metaValue = TextUtils.decolorize(serializedLocName);
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
				// Reconstruct from compact representation (no additional item metadata):
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

				// Skip loading the metadata if no further data (besides the item type) is given:
				if (itemDataData.size() <= 1) {
					itemDataData = null;
				}
			}
			assert itemType != null;

			// Create item stack (still misses metadata):
			ItemStack dataItem = new ItemStack(itemType);

			// Load additional metadata:
			if (itemDataData != null) {
				// Prepare the data for the metadata deserialization:
				// We (shallow) copy the data to a new Map, because we will have to insert
				// additional data for the ItemMeta to be deserializable, and don't want to modify
				// the given original data.
				// Note: Additional information (e.g. the item type) does not need to be removed,
				// but is simply ignored.
				Map<@NonNull String, @NonNull Object> itemMetaData = itemDataData.getValuesCopy();

				// Recursively replace all config sections with Maps, because the ItemMeta
				// deserialization expects Maps:
				ConfigUtils.convertSectionsToMaps(itemMetaData);

				// Determine the meta type:
				String metaType = getMetaType(itemType);
				if (metaType == null) {
					throw new InvalidDataException("Items of type " + itemType.name()
							+ " do not support metadata!");
				}

				// Insert meta type:
				itemMetaData.put(META_TYPE_KEY, metaType);

				// Convert color codes for display name and lore:
				Object displayNameData = itemMetaData.get(DISPLAY_NAME_KEY);
				if (displayNameData instanceof String) { // Also checks for null
					itemMetaData.put(
							DISPLAY_NAME_KEY,
							TextUtils.colorize((String) displayNameData)
					);
				}
				// Null if the data is not a list:
				List<?> loreData = itemDataData.getList(LORE_KEY);
				if (loreData != null) {
					itemMetaData.put(LORE_KEY, TextUtils.colorizeUnknown(loreData));
				}

				// Deserialize the ItemMeta (can be null):
				ItemMeta itemMeta = ItemSerialization.deserializeItemMeta(itemMetaData);

				// Apply the ItemMeta:
				dataItem.setItemMeta(itemMeta);
			}

			// Create ItemData:
			// Unmodifiable wrapper: Avoids creating another item copy during construction.
			ItemData itemData = new ItemData(UnmodifiableItemStack.ofNonNull(dataItem));
			return itemData;
		}
	};

	/////

	private final UnmodifiableItemStack dataItem; // Has an amount of 1
	// Cache serialized item metadata, to avoid serializing it again for every comparison:
	// Gets lazily initialized when needed.
	private @ReadOnly @Nullable Map<? extends @NonNull String, @ReadOnly @NonNull ?> serializedMetaData = null;

	public ItemData(Material type) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.ofNonNull(new ItemStack(type)));
	}

	// The display name and lore are expected to use Minecraft's color codes.
	public ItemData(
			Material type,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends @NonNull String> lore
	) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.ofNonNull(
				ItemUtils.createItemStack(type, 1, displayName, lore)
		));
	}

	public ItemData(
			ItemData otherItemData,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends @NonNull String> lore
	) {
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		this(UnmodifiableItemStack.ofNonNull(
				ItemUtils.createItemStack(otherItemData, 1, displayName, lore)
		));
	}

	/**
	 * Creates a new {@link ItemData} with the data of the given item stack.
	 * <p>
	 * The given item stack is copied before it is stored by the {@link ItemData}.
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
		this.dataItem = ItemUtils.unmodifiableCopyWithAmount(dataItem, 1);
	}

	public UnmodifiableItemStack asUnmodifiableItemStack() {
		return dataItem;
	}

	public Material getType() {
		return dataItem.getType();
	}

	public int getMaxStackSize() {
		return dataItem.getMaxStackSize();
	}

	// Creates a copy of this ItemData, but changes the item type. If the new type matches the
	// previous type, the current ItemData is returned.
	// Any incompatible metadata is removed.
	public ItemData withType(Material type) {
		Validate.notNull(type, "type is null");
		Validate.isTrue(type.isItem(), () -> "type is not an item: " + type);
		if (this.getType() == type) return this;
		ItemStack newDataItem = this.createItemStack();
		newDataItem.setType(type);
		// Unmodifiable wrapper: Avoids creating another item copy during construction.
		return new ItemData(UnmodifiableItemStack.ofNonNull(newDataItem));
	}

	// Not null.
	private Map<? extends @NonNull String, @NonNull ?> getSerializedMetaData() {
		// Lazily cache the serialized data:
		if (serializedMetaData == null) {
			ItemMeta itemMeta = dataItem.getItemMeta();
			// Not null after initialization:
			serializedMetaData = ItemSerialization.serializeItemMetaOrEmpty(itemMeta);
		}
		assert serializedMetaData != null;
		return serializedMetaData;
	}

	public boolean hasItemMeta() {
		return !this.getSerializedMetaData().isEmpty(); // Equivalent to dataItem.hasItemMeta()
	}

	public @Nullable ItemMeta getItemMeta() {
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

	public UnmodifiableItemStack createUnmodifiableItemStack(int amount) {
		return UnmodifiableItemStack.ofNonNull(this.createItemStack(amount));
	}

	public boolean isSimilar(@ReadOnly @Nullable ItemStack other) {
		return dataItem.isSimilar(other);
	}

	public boolean isSimilar(@Nullable UnmodifiableItemStack other) {
		return other != null && other.isSimilar(dataItem);
	}

	public boolean matches(@ReadOnly @Nullable ItemStack item) {
		return this.matches(item, false); // Not matching partial lists
	}

	public boolean matches(@Nullable UnmodifiableItemStack item) {
		return this.matches(ItemUtils.asItemStackOrNull(item));
	}

	public boolean matches(@ReadOnly @Nullable ItemStack item, boolean matchPartialLists) {
		// Same type and matching data:
		return ItemUtils.matchesData(
				item,
				this.getType(),
				this.getSerializedMetaData(),
				matchPartialLists
		);
	}

	public boolean matches(@Nullable UnmodifiableItemStack item, boolean matchPartialLists) {
		return this.matches(ItemUtils.asItemStackOrNull(item), matchPartialLists);
	}

	public boolean matches(@Nullable ItemData itemData) {
		return this.matches(itemData, false); // Not matching partial lists
	}

	// Given ItemData is of same type and has data matching this ItemData.
	public boolean matches(@Nullable ItemData itemData, boolean matchPartialLists) {
		if (itemData == null) return false;
		if (itemData.getType() != this.getType()) return false;
		return ItemUtils.matchesData(
				itemData.getSerializedMetaData(),
				this.getSerializedMetaData(),
				matchPartialLists
		);
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
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ItemData)) return false;
		ItemData other = (ItemData) obj;
		if (!dataItem.equals(other.dataItem)) return false;
		return true;
	}

	public Object serialize() {
		return Unsafe.assertNonNull(SERIALIZER.serialize(this));
	}
}
