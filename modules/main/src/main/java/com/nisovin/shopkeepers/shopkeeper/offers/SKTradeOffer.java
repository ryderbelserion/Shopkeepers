package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
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

	public static void saveToConfig(ConfigurationSection config, String node, @ReadOnly Collection<? extends TradeOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 1;
		for (TradeOffer offer : offers) {
			// These items are assumed to be immutable.
			UnmodifiableItemStack item1 = offer.getItem1();
			UnmodifiableItemStack item2 = offer.getItem2();
			UnmodifiableItemStack resultItem = offer.getResultItem();

			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			offerSection.set("item1", item1);
			offerSection.set("item2", item2);
			offerSection.set("resultItem", resultItem);
			id++;
		}
	}

	// Elements inside the config section are assumed to be immutable and can be reused without having to be copied.
	public static List<? extends TradeOffer> loadFromConfig(ConfigurationSection config, String node, String errorContext) {
		List<TradeOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) {
					// Invalid offer: Not a section.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid trading offer section for " + key));
					continue;
				}

				// The item stacks are assumed to be immutable and therefore do not need to be copied.
				UnmodifiableItemStack resultItem = ConfigUtils.loadUnmodifiableItemStack(offerSection, "resultItem");
				UnmodifiableItemStack item1 = ConfigUtils.loadUnmodifiableItemStack(offerSection, "item1");
				UnmodifiableItemStack item2 = ConfigUtils.loadUnmodifiableItemStack(offerSection, "item2");
				if (ItemUtils.isEmpty(resultItem) || ItemUtils.isEmpty(item1)) {
					// Invalid offer.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid trading offer for " + key + ": item1 or resultItem is empty"));
					continue;
				}
				offers.add(new SKTradeOffer(resultItem, item1, item2));
			}
		}

		return offers;
	}

	// Note: Returns the same list instance if no items were migrated.
	public static List<? extends TradeOffer> migrateItems(@ReadOnly List<? extends TradeOffer> offers, String errorContext) {
		if (offers == null) return null;
		List<TradeOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			TradeOffer offer = offers.get(i);
			if (offer == null) continue; // Skip invalid entries

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
				}
				resultItem = migratedResultItem;
				itemsMigrated = true;
			}
			UnmodifiableItemStack migratedItem1 = ItemMigration.migrateItemStack(item1);
			if (!ItemUtils.isSimilar(item1, migratedItem1)) {
				if (ItemUtils.isEmpty(migratedItem1) && !ItemUtils.isEmpty(item1)) {
					migrationFailed = true;
				}
				item1 = migratedItem1;
				itemsMigrated = true;
			}
			UnmodifiableItemStack migratedItem2 = ItemMigration.migrateItemStack(item2);
			if (!ItemUtils.isSimilar(item2, migratedItem2)) {
				if (ItemUtils.isEmpty(migratedItem2) && !ItemUtils.isEmpty(item2)) {
					migrationFailed = true;
				}
				item2 = migratedItem2;
				itemsMigrated = true;
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

				if (migrationFailed) {
					Log.warning(StringUtils.prefix(errorContext, ": ", "Trading offer item migration failed for offer "
							+ (i + 1) + ": " + offer.toString()));
					continue; // Skip this offer
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
