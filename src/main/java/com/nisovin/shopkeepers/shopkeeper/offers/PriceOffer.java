package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Stores information about an item stack being sold or bought for a certain price.
 */
public class PriceOffer {

	private final ItemStack item; // not null/empty
	private final int price; // > 0

	public PriceOffer(ItemStack item, int price) {
		Validate.isTrue(!ItemUtils.isEmpty(item), "Item cannot be empty!");
		Validate.isTrue(price > 0, "Price has to be positive!");
		this.item = item.clone();
		this.price = price;
	}

	public ItemStack getItem() {
		return item.clone();
	}

	public int getPrice() {
		return price;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<PriceOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (PriceOffer offer : offers) {
			ItemStack item = offer.getItem(); // is a clone
			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			offerSection.set("item", item);
			offerSection.set("price", offer.getPrice());
			id++;
		}
	}

	public static List<PriceOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<PriceOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String id : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(id);
				if (offerSection == null) continue; // invalid offer: not a section
				ItemStack item = offerSection.getItemStack("item");
				int price = offerSection.getInt("price");
				if (ItemUtils.isEmpty(item) || price <= 0) continue; // invalid offer
				offers.add(new PriceOffer(item, price));
			}
		}
		return offers;
	}

	// legacy:

	/*public static void saveToConfigOld(ConfigurationSection config, String node, Collection<PriceOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (PriceOffer offer : offers) {
			ItemStack item = offer.getItem();
			ConfigurationSection offerSection = offersSection.createSection(id + "");
			offerSection.set("item", item);
			String attributes = NMSManager.getProvider().saveItemAttributesToString(item);
			if (attributes != null && !attributes.isEmpty()) {
				offerSection.set("attributes", attributes);
			}
			// legacy: amount was stored separately from the item
			offerSection.set("amount", item.getAmount());
			offerSection.set("cost", offer.getPrice());
			id++;
		}
	}*/

	public static List<PriceOffer> loadFromConfigOld(ConfigurationSection config, String node) {
		List<PriceOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) continue; // invalid offer: not a section
				ItemStack item = offerSection.getItemStack("item");
				if (item != null) {
					// legacy: the amount was stored separately from the item
					item.setAmount(offerSection.getInt("amount", 1));
				}
				int price = offerSection.getInt("cost");
				if (ItemUtils.isEmpty(item) || price <= 0) continue; // invalid offer
				offers.add(new PriceOffer(item, price));
			}
		}
		return offers;
	}
}
