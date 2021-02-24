package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;

// Shares its implementation with SKTradingRecipe, but always reports to not be out of stock.
public class SKTradeOffer extends SKTradingRecipe implements TradeOffer {

	public SKTradeOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
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

	public static void saveToConfig(ConfigurationSection config, String node, Collection<? extends TradeOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 1;
		for (TradeOffer offer : offers) {
			// Note: The items are clones.
			ItemStack item1 = offer.getItem1();
			ItemStack item2 = offer.getItem2();
			ItemStack resultItem = offer.getResultItem();

			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			offerSection.set("item1", item1);
			offerSection.set("item2", item2);
			offerSection.set("resultItem", resultItem);
			id++;
		}
	}

	public static List<SKTradeOffer> loadFromConfig(ConfigurationSection config, String node, String errorContext) {
		List<SKTradeOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) {
					// Invalid offer: Not a section.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid trading offer section for " + key));
					continue;
				}
				ItemStack resultItem = offerSection.getItemStack("resultItem");
				ItemStack item1 = offerSection.getItemStack("item1");
				ItemStack item2 = offerSection.getItemStack("item2");
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
	public static List<SKTradeOffer> migrateItems(List<SKTradeOffer> offers, String errorContext) {
		if (offers == null) return null;
		List<SKTradeOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			SKTradeOffer offer = offers.get(i);
			if (offer == null) continue; // Skip invalid entries

			boolean itemsMigrated = false;
			boolean migrationFailed = false;

			// Note: The items are clones.
			ItemStack resultItem = offer.getResultItem();
			ItemStack item1 = offer.getItem1();
			ItemStack item2 = offer.getItem2();

			ItemStack migratedResultItem = ItemUtils.migrateItemStack(resultItem);
			if (!ItemUtils.isSimilar(resultItem, migratedResultItem)) {
				if (ItemUtils.isEmpty(migratedResultItem) && !ItemUtils.isEmpty(resultItem)) {
					migrationFailed = true;
				}
				resultItem = migratedResultItem;
				itemsMigrated = true;
			}
			ItemStack migratedItem1 = ItemUtils.migrateItemStack(item1);
			if (!ItemUtils.isSimilar(item1, migratedItem1)) {
				if (ItemUtils.isEmpty(migratedItem1) && !ItemUtils.isEmpty(item1)) {
					migrationFailed = true;
				}
				item1 = migratedItem1;
				itemsMigrated = true;
			}
			ItemStack migratedItem2 = ItemUtils.migrateItemStack(item2);
			if (!ItemUtils.isSimilar(item2, migratedItem2)) {
				if (ItemUtils.isEmpty(migratedItem2) && !ItemUtils.isEmpty(item2)) {
					migrationFailed = true;
				}
				item2 = migratedItem2;
				itemsMigrated = true;
			}

			if (itemsMigrated) {
				if (migratedOffers == null) {
					migratedOffers = new ArrayList<>(size);
					for (int j = 0; j < i; ++j) {
						SKTradeOffer oldOffer = offers.get(j);
						if (oldOffer == null) continue; // Skip invalid entries
						migratedOffers.add(oldOffer);
					}
				}

				if (migrationFailed) {
					Log.warning(StringUtils.prefix(errorContext, ": ", "Trading offer item migration failed for offer "
							+ (i + 1) + ": " + offer.toString()));
					continue; // Skip this offer
				}
				assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(item1);
				migratedOffers.add(new SKTradeOffer(resultItem, item1, item2));
			}
		}
		return (migratedOffers == null) ? offers : migratedOffers;
	}
}
