package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DataValue;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.bukkit.ItemStackValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ItemStackSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

// Shares its implementation with SKTradingRecipe, but always reports to not be out of stock.
public class SKTradeOffer extends SKTradingRecipe implements TradeOffer {

	/**
	 * Creates a new {@link SKTradeOffer}.
	 * <p>
	 * If the given item stacks are {@link UnmodifiableItemStack}s, they are assumed to be immutable and therefore not
	 * copied before they are stored by the trade offer. Otherwise, they are first copied.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public SKTradeOffer(@ReadOnly ItemStack resultItem, @ReadOnly ItemStack item1, @ReadOnly ItemStack item2) {
		super(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link SKTradeOffer}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are stored by the trade
	 * offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public SKTradeOffer(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		super(resultItem, item1, item2);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKTradeOffer [resultItem=");
		builder.append(resultItem);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (!(obj instanceof SKTradeOffer)) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	/**
	 * Creates a {@link TradingRecipe} based on the given trade offer.
	 * 
	 * @param offer
	 *            the trade offer
	 * @param outOfStock
	 *            whether to mark the trading recipe as being out of stock
	 * @return the trading recipe
	 */
	public static TradingRecipe toTradingRecipe(TradeOffer offer, boolean outOfStock) {
		// The items of the trade offer are immutable, so they do not need to be copied.
		return new SKTradingRecipe(offer.getResultItem(), offer.getItem1(), offer.getItem2(), outOfStock);
	}

	private static final Property<UnmodifiableItemStack> RESULT_ITEM = new BasicProperty<UnmodifiableItemStack>()
			.dataKeyAccessor("resultItem", ItemStackSerializers.UNMODIFIABLE)
			.validator(ItemStackValidators.Unmodifiable.NON_EMPTY)
			.build();
	private static final Property<UnmodifiableItemStack> ITEM1 = new BasicProperty<UnmodifiableItemStack>()
			.dataKeyAccessor("item1", ItemStackSerializers.UNMODIFIABLE)
			.validator(ItemStackValidators.Unmodifiable.NON_EMPTY)
			.build();
	private static final Property<UnmodifiableItemStack> ITEM2 = new BasicProperty<UnmodifiableItemStack>()
			.dataKeyAccessor("item2", ItemStackSerializers.UNMODIFIABLE)
			.validator(ItemStackValidators.Unmodifiable.NON_EMPTY)
			.nullable()
			.defaultValue(null)
			.build();

	/**
	 * A {@link DataSerializer} for values of type {@link TradeOffer}.
	 */
	public static final DataSerializer<TradeOffer> SERIALIZER = new DataSerializer<TradeOffer>() {
		@Override
		public Object serialize(TradeOffer value) {
			Validate.notNull(value, "value is null");
			DataContainer offerData = DataContainer.create();
			// The items are assumed to be immutable.
			offerData.set(RESULT_ITEM, value.getResultItem());
			offerData.set(ITEM1, value.getItem1());
			offerData.set(ITEM2, value.getItem2()); // Can be null
			return offerData.serialize();
		}

		@Override
		public TradeOffer deserialize(Object data) throws InvalidDataException {
			DataContainer offerData = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				// The item stacks are assumed to be immutable and therefore do not need to be copied.
				UnmodifiableItemStack resultItem = offerData.get(RESULT_ITEM);
				UnmodifiableItemStack item1 = offerData.get(ITEM1);
				UnmodifiableItemStack item2 = offerData.get(ITEM2); // Can be null
				return new SKTradeOffer(resultItem, item1, item2);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link TradeOffer}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<@ReadOnly List<? extends TradeOffer>> LIST_SERIALIZER = new DataSerializer<List<? extends TradeOffer>>() {
		@Override
		public Object serialize(List<? extends TradeOffer> value) {
			Validate.notNull(value, "value is null");
			DataContainer offerListData = DataContainer.create();
			int id = 1;
			for (TradeOffer offer : value) {
				Validate.notNull(offer, "list of offers contains null");
				offerListData.set(String.valueOf(id), SERIALIZER.serialize(offer));
				id++;
			}
			return offerListData.serialize();
		}

		@Override
		public List<? extends TradeOffer> deserialize(Object data) throws InvalidDataException {
			DataContainer offerListData = DataContainerSerializers.DEFAULT.deserialize(data);
			Set<String> keys = offerListData.getKeys();
			List<TradeOffer> offers = new ArrayList<>(keys.size());
			for (String id : keys) {
				Object offerData = offerListData.get(id);
				TradeOffer offer;
				try {
					offer = SERIALIZER.deserialize(offerData);
				} catch (InvalidDataException e) {
					throw new InvalidDataException("Invalid trade offer " + id + ": " + e.getMessage(), e);
				}
				offers.add(offer);
			}
			return offers;
		}
	};

	public static void saveOffers(DataValue dataValue, @ReadOnly List<? extends TradeOffer> offers) {
		Validate.notNull(dataValue, "dataValue is null");
		if (offers == null) {
			dataValue.clear();
			return;
		}

		Object offerListData = LIST_SERIALIZER.serialize(offers);
		dataValue.set(offerListData);
	}

	public static List<? extends TradeOffer> loadOffers(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");

		if (!dataValue.isPresent()) {
			// No data. -> Return an empty list of offers.
			return Collections.emptyList();
		}

		Object offerListData = dataValue.get();
		return LIST_SERIALIZER.deserialize(offerListData);
	}

	// Returns true if the data has changed due to migrations.
	public static boolean migrateOffers(DataValue dataValue, String logPrefix) throws InvalidDataException {
		Validate.notNull(logPrefix, "logPrefix is null");
		List<? extends TradeOffer> offers = loadOffers(dataValue);
		List<? extends TradeOffer> migratedOffers = migrateItems(offers);
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
	private static List<? extends TradeOffer> migrateItems(@ReadOnly List<? extends TradeOffer> offers) throws InvalidDataException {
		Validate.notNull(offers, "offers is null");
		assert !offers.contains(null);
		List<TradeOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			TradeOffer offer = offers.get(i);
			assert offer != null;

			boolean itemsMigrated = false;
			boolean migrationFailed = false;

			// These items are assumed to be immutable.
			UnmodifiableItemStack resultItem = offer.getResultItem();
			UnmodifiableItemStack item1 = offer.getItem1();
			UnmodifiableItemStack item2 = offer.getItem2();

			UnmodifiableItemStack migratedResultItem = ItemMigration.migrateItemStack(resultItem);
			if (!ItemUtils.isSimilar(resultItem, migratedResultItem)) {
				if (ItemUtils.isEmpty(migratedResultItem) && !ItemUtils.isEmpty(resultItem)) {
					migrationFailed = true;
				} else {
					resultItem = migratedResultItem;
					itemsMigrated = true;
				}
			}
			UnmodifiableItemStack migratedItem1 = ItemMigration.migrateItemStack(item1);
			if (!ItemUtils.isSimilar(item1, migratedItem1)) {
				if (ItemUtils.isEmpty(migratedItem1) && !ItemUtils.isEmpty(item1)) {
					migrationFailed = true;
				} else {
					item1 = migratedItem1;
					itemsMigrated = true;
				}
			}
			UnmodifiableItemStack migratedItem2 = ItemMigration.migrateItemStack(item2);
			if (!ItemUtils.isSimilar(item2, migratedItem2)) {
				if (ItemUtils.isEmpty(migratedItem2) && !ItemUtils.isEmpty(item2)) {
					migrationFailed = true;
				} else {
					item2 = migratedItem2;
					itemsMigrated = true;
				}
			}

			if (migrationFailed) {
				throw new InvalidDataException("Item migration failed for trade offer " + (i + 1) + ": " + offer);
			}

			if (itemsMigrated) {
				// Lazily setup the list of migrated offers, and add the trades that were already processed but did not
				// require migrations:
				if (migratedOffers == null) {
					migratedOffers = new ArrayList<>(size);
					for (int j = 0; j < i; ++j) {
						TradeOffer oldOffer = offers.get(j);
						if (oldOffer == null) continue; // Skip invalid entries
						migratedOffers.add(oldOffer);
					}
				}

				// Add the migrated offer to the list of migrated offers:
				assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(item1);
				migratedOffers.add(new SKTradeOffer(resultItem, item1, item2));
			} else if (migratedOffers != null) {
				// Add the previous offer, which did not require any migrations, to the list of already migrated offers:
				migratedOffers.add(offer);
			}
		}
		return (migratedOffers == null) ? offers : migratedOffers;
	}
}
