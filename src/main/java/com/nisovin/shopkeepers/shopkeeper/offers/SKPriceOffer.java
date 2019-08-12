package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.util.ItemUtils;

public class SKPriceOffer implements PriceOffer {

	private final ItemStack item; // not null/empty
	private final int price; // > 0

	public SKPriceOffer(ItemStack item, int price) {
		Validate.isTrue(!ItemUtils.isEmpty(item), "Item cannot be empty!");
		Validate.isTrue(price > 0, "Price has to be positive!");
		this.item = item.clone();
		this.price = price;
	}

	@Override
	public ItemStack getItem() {
		return item.clone();
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
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKPriceOffer)) return false;
		SKPriceOffer other = (SKPriceOffer) obj;
		if (!item.equals(other.item)) return false;
		if (price != other.price) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<? extends PriceOffer> offers) {
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

	public static List<SKPriceOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<SKPriceOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String id : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(id);
				if (offerSection == null) continue; // invalid offer: not a section
				ItemStack item = offerSection.getItemStack("item");
				int price = offerSection.getInt("price");
				if (ItemUtils.isEmpty(item) || price <= 0) continue; // invalid offer
				offers.add(new SKPriceOffer(item, price));
			}
		}
		return offers;
	}
}
