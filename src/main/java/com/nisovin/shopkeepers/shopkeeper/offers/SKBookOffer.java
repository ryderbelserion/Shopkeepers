package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;

public class SKBookOffer implements BookOffer {

	private final String bookTitle; // not null or empty
	private final int price; // > 0

	public SKBookOffer(String bookTitle, int price) {
		Validate.notEmpty(bookTitle, "Book title cannot be null or empty!");
		Validate.isTrue(price > 0, "Price has to be positive!");
		this.bookTitle = bookTitle;
		this.price = price;
	}

	@Override
	public String getBookTitle() {
		return bookTitle;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKBookOffer [bookTitle=");
		builder.append(bookTitle);
		builder.append(", price=");
		builder.append(price);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bookTitle.hashCode();
		result = prime * result + price;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKBookOffer)) return false;
		SKBookOffer other = (SKBookOffer) obj;
		if (!bookTitle.equals(other.bookTitle)) return false;
		if (price != other.price) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<? extends BookOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		for (BookOffer offer : offers) {
			offersSection.set(offer.getBookTitle(), offer.getPrice());
		}
	}

	public static List<SKBookOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<SKBookOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String bookTitle : offersSection.getKeys(false)) {
				int price = offersSection.getInt(bookTitle);
				if (bookTitle == null || price <= 0) continue; // invalid offer
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}
}
