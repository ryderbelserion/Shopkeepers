package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKBookOffer implements BookOffer {

	private final String bookTitle; // not null or empty
	private final int price; // > 0

	public SKBookOffer(String bookTitle, int price) {
		Validate.notEmpty(bookTitle, "bookTitle is null or empty");
		Validate.isTrue(price > 0, "price has to be positive");
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

	public static void save(DataContainer dataContainer, String node, Collection<? extends BookOffer> offers) {
		DataContainer offerListData = dataContainer.createContainer(node);
		int id = 1;
		for (BookOffer offer : offers) {
			DataContainer offerData = offerListData.createContainer(String.valueOf(id));
			offerData.set("book", offer.getBookTitle());
			offerData.set("price", offer.getPrice());
			id++;
		}
	}

	// Elements inside the data container are assumed to be immutable and can be reused without having to be copied.
	public static List<? extends BookOffer> load(DataContainer dataContainer, String node, String errorPrefix) {
		if (errorPrefix == null) errorPrefix = "";
		List<BookOffer> offers = new ArrayList<>();
		DataContainer offerListData = dataContainer.getContainer(node);
		if (offerListData != null) {
			for (String id : offerListData.getKeys()) {
				DataContainer offerData = offerListData.getContainer(id);
				if (offerData == null) {
					// Invalid offer: Not a container.
					Log.warning(errorPrefix + "Invalid data for book offer " + id);
					continue;
				}
				String bookTitle = offerData.getString("book");
				int price = offerData.getInt("price");
				if (StringUtils.isEmpty(bookTitle)) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid book offer " + id + ": Book title is empty.");
					continue;
				}
				if (price <= 0) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid book offer " + id + ": Price has to be positive, but is "
							+ price + ".");
					continue;
				}
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}

	// TODO Legacy, remove again at some point (changed during MC 1.14.4).
	public static List<? extends BookOffer> loadFromLegacyData(DataContainer dataContainer, String node, String errorPrefix) {
		if (errorPrefix == null) errorPrefix = "";
		List<BookOffer> offers = new ArrayList<>();
		DataContainer offerListData = dataContainer.getContainer(node);
		if (offerListData != null) {
			for (String bookTitle : offerListData.getKeys()) {
				if (offerListData.isContainer(bookTitle)) {
					// Found a container instead of an integer. -> Probably already uses the new data format.
					continue; // Skip
				}
				if (StringUtils.isEmpty(bookTitle)) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid book offer: Book title is empty.");
					continue;
				}
				int price = offerListData.getInt(bookTitle);
				if (price <= 0) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid book offer for '" + bookTitle
							+ "': Price has to be positive, but is " + price + ".");
					continue;
				}
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}
}
