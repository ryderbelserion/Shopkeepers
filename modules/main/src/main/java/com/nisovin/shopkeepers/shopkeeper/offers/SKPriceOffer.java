package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.value.DataValue;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.bukkit.ItemStackValidators;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ItemStackSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKPriceOffer implements PriceOffer {

	private final UnmodifiableItemStack item; // Not null or empty, assumed immutable
	private final int price; // > 0

	/**
	 * Creates a new {@link SKPriceOffer}.
	 * <p>
	 * If the given item stack is an {@link UnmodifiableItemStack}, it is assumed to be immutable
	 * and therefore not copied before it is stored by the price offer. Otherwise, it is first
	 * copied.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 */
	public SKPriceOffer(ItemStack item, int price) {
		this(ItemUtils.nonNullUnmodifiableCloneIfModifiable(item), price);
	}

	/**
	 * Creates a new {@link SKPriceOffer}.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored
	 * by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 */
	public SKPriceOffer(UnmodifiableItemStack item, int price) {
		Validate.isTrue(!ItemUtils.isEmpty(item), "item is empty");
		Validate.isTrue(price > 0, "price has to be positive");
		this.item = item;
		this.price = price;
	}

	@Override
	public UnmodifiableItemStack getItem() {
		return item;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKPriceOffer [item=");
		builder.append(item);
		builder.append(", price=");
		builder.append(price);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + item.hashCode();
		result = prime * result + price;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKPriceOffer)) return false;
		SKPriceOffer other = (SKPriceOffer) obj;
		if (price != other.price) return false;
		if (!item.equals(other.item)) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	private static final Property<@NonNull UnmodifiableItemStack> ITEM = new BasicProperty<@NonNull UnmodifiableItemStack>()
			.dataKeyAccessor("item", ItemStackSerializers.UNMODIFIABLE)
			.validator(ItemStackValidators.Unmodifiable.NON_EMPTY)
			.build();
	private static final Property<@NonNull Integer> PRICE = new BasicProperty<@NonNull Integer>()
			.dataKeyAccessor("price", NumberSerializers.INTEGER)
			.validator(IntegerValidators.POSITIVE)
			.build();

	/**
	 * A {@link DataSerializer} for values of type {@link PriceOffer}.
	 */
	public static final DataSerializer<@NonNull PriceOffer> SERIALIZER = new DataSerializer<@NonNull PriceOffer>() {
		@Override
		public @Nullable Object serialize(PriceOffer value) {
			Validate.notNull(value, "value is null");
			DataContainer offerData = DataContainer.create();
			offerData.set(ITEM, value.getItem()); // Assumed immutable
			offerData.set(PRICE, value.getPrice());
			return offerData.serialize();
		}

		@Override
		public PriceOffer deserialize(Object data) throws InvalidDataException {
			DataContainer offerData = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				// The item stack is assumed to be immutable and therefore does not need to be
				// copied.
				UnmodifiableItemStack item = offerData.get(ITEM);
				int price = offerData.get(PRICE);
				return new SKPriceOffer(item, price);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link PriceOffer}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<@NonNull List<? extends @NonNull PriceOffer>> LIST_SERIALIZER = new DataSerializer<@NonNull List<? extends @NonNull PriceOffer>>() {
		@Override
		public @Nullable Object serialize(@ReadOnly List<? extends @NonNull PriceOffer> value) {
			Validate.notNull(value, "value is null");
			DataContainer offerListData = DataContainer.create();
			int id = 1;
			for (PriceOffer offer : value) {
				Validate.notNull(offer, "list of offers contains null");
				offerListData.set(String.valueOf(id), SERIALIZER.serialize(offer));
				id++;
			}
			return offerListData.serialize();
		}

		@Override
		public List<? extends @NonNull PriceOffer> deserialize(
				Object data
		) throws InvalidDataException {
			DataContainer offerListData = DataContainerSerializers.DEFAULT.deserialize(data);
			Set<? extends @NonNull String> keys = offerListData.getKeys();
			List<@NonNull PriceOffer> offers = new ArrayList<>(keys.size());
			for (String id : keys) {
				Object offerData = Unsafe.assertNonNull(offerListData.get(id));
				PriceOffer offer;
				try {
					offer = SERIALIZER.deserialize(offerData);
				} catch (InvalidDataException e) {
					throw new InvalidDataException("Invalid price offer " + id + ": "
							+ e.getMessage(), e);
				}
				offers.add(offer);
			}
			return offers;
		}
	};

	public static void saveOffers(
			DataValue dataValue,
			@ReadOnly @Nullable List<? extends @NonNull PriceOffer> offers
	) {
		Validate.notNull(dataValue, "dataValue is null");
		if (offers == null) {
			dataValue.clear();
			return;
		}

		Object offerListData = LIST_SERIALIZER.serialize(offers);
		dataValue.set(offerListData);
	}

	public static List<? extends @NonNull PriceOffer> loadOffers(DataValue dataValue)
			throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");
		Object offerListData = dataValue.get();
		if (offerListData == null) {
			// No data. -> Return an empty list of offers.
			return Collections.emptyList();
		}
		return LIST_SERIALIZER.deserialize(offerListData);
	}

	// Returns true if the data has changed due to migrations.
	public static boolean migrateOffers(DataValue dataValue, String logPrefix)
			throws InvalidDataException {
		Validate.notNull(logPrefix, "logPrefix is null");
		List<? extends @NonNull PriceOffer> offers = loadOffers(dataValue);
		List<? extends @NonNull PriceOffer> migratedOffers = migrateItems(offers, logPrefix);
		if (offers == migratedOffers) {
			// No offers were migrated.
			return false;
		}

		// Write back the migrated offers:
		saveOffers(dataValue, migratedOffers);
		Log.debug(DebugOptions.itemMigrations, () -> logPrefix + "Migrated items of trade offers.");
		return true;
	}

	// Note: Returns the same list instance if no items were migrated.
	private static List<? extends @NonNull PriceOffer> migrateItems(
			@ReadOnly List<? extends @NonNull PriceOffer> offers,
			String logPrefix
	) throws InvalidDataException {
		Validate.notNull(offers, "offers is null");
		assert !CollectionUtils.containsNull(offers);
		List<@NonNull PriceOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			PriceOffer offer = offers.get(i);
			assert offer != null;

			boolean itemsMigrated = false;

			@NonNull UnmodifiableItemStack item = offer.getItem();
			assert !ItemUtils.isEmpty(item);
			UnmodifiableItemStack migratedItem = ItemMigration.migrateItemStack(item);
			if (!ItemUtils.isSimilar(item, migratedItem)) {
				if (ItemUtils.isEmpty(migratedItem)) {
					throw new InvalidDataException("Item migration failed for price offer "
							+ (i + 1) + ": " + offer);
				} else {
					item = Unsafe.assertNonNull(migratedItem);
					itemsMigrated = true;
				}
			}

			if (Settings.addTagToShopCreationItemsInShops
					&& Settings.shopCreationItem.matches(item)) {
				ItemStack shopCreationItemWithTag = item.copy();
				if (ShopCreationItem.addTag(shopCreationItemWithTag)) {
					item = UnmodifiableItemStack.ofNonNull(shopCreationItemWithTag);
					itemsMigrated = true;

					final int offerId = i + 1;
					Log.debug(DebugOptions.itemMigrations,
							() -> logPrefix + "Tag added to shop creation item in trade offer "
									+ offerId);
				}
			}

			if (itemsMigrated) {
				// Lazily set up the list of migrated offers, and add the trades that were already
				// processed but did not require migrations:
				if (migratedOffers == null) {
					migratedOffers = new ArrayList<>(size);
					for (int j = 0; j < i; ++j) {
						PriceOffer oldOffer = offers.get(j);
						assert oldOffer != null;
						migratedOffers.add(oldOffer);
					}
				}

				// Add the migrated offer to the list of migrated offers:
				assert !ItemUtils.isEmpty(item);
				migratedOffers.add(new SKPriceOffer(item, offer.getPrice()));
			} else if (migratedOffers != null) {
				// Add the previous offer, which did not require any migrations, to the list of
				// already migrated offers:
				migratedOffers.add(offer);
			}
		}
		return (migratedOffers == null) ? offers : migratedOffers;
	}
}
