package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

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
		int id = 1;
		for (BookOffer offer : offers) {
			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			offerSection.set("book", offer.getBookTitle());
			offerSection.set("price", offer.getPrice());
			id++;
		}
	}

	public static List<? extends BookOffer> loadFromConfig(ConfigurationSection config, String node, String errorContext) {
		List<BookOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) continue; // Invalid offer: Not a section.
				String bookTitle = offerSection.getString("book");
				int price = offerSection.getInt("price");
				if (StringUtils.isEmpty(bookTitle)) {
					// Invalid offer.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid book offer: book title is empty"));
					continue;
				}
				if (price <= 0) {
					// Invalid offer.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid book offer for '" + bookTitle + "': price has to be positive but is " + price));
					continue;
				}
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}

	// TODO Legacy, remove again at some point (changed during MC 1.14.4).
	public static List<? extends BookOffer> loadFromLegacyConfig(ConfigurationSection config, String node, String errorContext) {
		List<BookOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String bookTitle : offersSection.getKeys(false)) {
				if (offersSection.isConfigurationSection(bookTitle)) {
					// Found a config section instead of an integer. -> Probably already uses the new data format.
					continue; // Skip
				}
				int price = offersSection.getInt(bookTitle);
				if (StringUtils.isEmpty(bookTitle)) {
					// Invalid offer.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid book offer: bookTitle is empty"));
					continue;
				}
				if (price <= 0) {
					// Invalid offer.
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid book offer for '" + bookTitle + "': price has to be positive but is " + price));
					continue;
				}
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}
}
