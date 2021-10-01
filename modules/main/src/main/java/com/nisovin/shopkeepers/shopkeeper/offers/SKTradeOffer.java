package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.DataUtils;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DataValue;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

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

	public static void saveOffers(DataValue dataValue, @ReadOnly Collection<? extends TradeOffer> offers) {
		Validate.notNull(dataValue, "dataValue is null");
		if (offers == null) {
			dataValue.clear();
			return;
		}

		DataContainer offerListData = dataValue.createContainer();
		int id = 1;
		for (TradeOffer offer : offers) {
			// These items are assumed to be immutable.
			UnmodifiableItemStack item1 = offer.getItem1();
			UnmodifiableItemStack item2 = offer.getItem2();
			UnmodifiableItemStack resultItem = offer.getResultItem();

			DataContainer offerData = offerListData.createContainer(String.valueOf(id));
			DataUtils.saveItemStack(offerData, "item1", item1);
			DataUtils.saveItemStack(offerData, "item2", item2);
			DataUtils.saveItemStack(offerData, "resultItem", resultItem);
			id++;
		}
	}

	// Elements inside the data are assumed to be immutable and can be reused without having to be copied.
	public static List<? extends TradeOffer> loadOffers(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");

		if (!dataValue.isPresent()) {
			// No data -> Return an empty list of offers.
			return new ArrayList<>(0);
		}

		DataContainer offerListData = dataValue.getContainer();
		if (offerListData == null) {
			throw new InvalidDataException("Invalid trade offer list data: " + dataValue.get());
		}

		List<TradeOffer> offers = new ArrayList<>();
		for (String id : offerListData.getKeys()) {
			DataContainer offerData = offerListData.getContainer(id);
			if (offerData == null) {
				// Data is not a container.
				throw new InvalidDataException("Invalid data for trade offer " + id);
			}

			// The item stacks are assumed to be immutable and therefore do not need to be copied.
			UnmodifiableItemStack resultItem = DataUtils.loadUnmodifiableItemStack(offerData, "resultItem");
			UnmodifiableItemStack item1 = DataUtils.loadUnmodifiableItemStack(offerData, "item1");
			UnmodifiableItemStack item2 = DataUtils.loadUnmodifiableItemStack(offerData, "item2");
			if (ItemUtils.isEmpty(resultItem) || ItemUtils.isEmpty(item1)) {
				throw new InvalidDataException("Invalid trade offer " + id + ": item1 or resultItem is empty.");
			}
			offers.add(new SKTradeOffer(resultItem, item1, item2));
		}
		return offers;
	}

	// Returns true if the data has changed due to migrations.
	public static boolean migrateOffers(DataValue dataValue) throws InvalidDataException {
		List<? extends TradeOffer> offers = loadOffers(dataValue);
		List<? extends TradeOffer> migratedOffers = migrateItems(offers);
		if (offers == migratedOffers) {
			// Nothing migrated.
			return false;
		}

		// Write back the migrated data:
		saveOffers(dataValue, migratedOffers);
		return true;
	}

	// Note: Returns the same list instance if no items were migrated.
	private static List<? extends TradeOffer> migrateItems(@ReadOnly List<? extends TradeOffer> offers) throws InvalidDataException {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
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
