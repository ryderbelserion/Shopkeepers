package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Stores information about up to two items being traded for another item.
 */
public class TradingOffer extends SKTradingRecipe {
	// shares its implementation with TradingRecipe, but always reports to not be out of stock

	public TradingOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		super(resultItem, item1, item2);
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<TradingOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (TradingOffer offer : offers) {
			// note: the items are clones
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

	public static List<TradingOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<TradingOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) continue; // invalid offer: not a section
				ItemStack resultItem = offerSection.getItemStack("resultItem");
				ItemStack item1 = offerSection.getItemStack("item1");
				ItemStack item2 = offerSection.getItemStack("item2");
				if (ItemUtils.isEmpty(resultItem) || ItemUtils.isEmpty(item1)) continue; // invalid offer
				offers.add(new TradingOffer(resultItem, item1, item2));
			}
		}
		return offers;
	}
}
