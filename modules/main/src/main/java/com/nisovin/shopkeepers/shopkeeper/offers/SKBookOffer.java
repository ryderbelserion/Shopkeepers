package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DataValue;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

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

	public static void saveOffers(DataValue dataValue, Collection<? extends BookOffer> offers) {
		Validate.notNull(dataValue, "dataValue is null");
		if (offers == null) {
			dataValue.clear();
			return;
		}

		DataContainer offerListData = dataValue.createContainer();
		int id = 1;
		for (BookOffer offer : offers) {
			DataContainer offerData = offerListData.createContainer(String.valueOf(id));
			offerData.set("book", offer.getBookTitle());
			offerData.set("price", offer.getPrice());
			id++;
		}
	}

	// Elements inside the data are assumed to be immutable and can be reused without having to be copied.
	public static List<? extends BookOffer> loadOffers(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");

		if (!dataValue.isPresent()) {
			// No data -> Return an empty list of offers.
			return new ArrayList<>(0);
		}

		DataContainer offerListData = dataValue.getContainer();
		if (offerListData == null) {
			throw new InvalidDataException("Invalid book offer list data: " + dataValue.get());
		}

		List<BookOffer> offers = new ArrayList<>();
		for (String id : offerListData.getKeys()) {
			DataContainer offerData = offerListData.getContainer(id);
			if (offerData == null) {
				// Data is not a container.
				throw new InvalidDataException("Invalid data for book offer " + id);
			}
			String bookTitle = offerData.getString("book");
			int price = offerData.getInt("price");
			if (StringUtils.isEmpty(bookTitle)) {
				throw new InvalidDataException("Invalid book offer " + id + ": Book title is empty.");
			}
			if (price <= 0) {
				throw new InvalidDataException("Invalid book offer " + id + ": Price has to be positive, but is " + price + ".");
			}
			offers.add(new SKBookOffer(bookTitle, price));
		}
		return offers;
	}

	// TODO Legacy, remove again at some point (changed during MC 1.14.4).
	public static List<? extends BookOffer> loadFromLegacyData(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");

		List<BookOffer> offers = new ArrayList<>();
		DataContainer offerListData = dataValue.getContainer();
		if (offerListData != null) {
			for (String bookTitle : offerListData.getKeys()) {
				if (offerListData.isContainer(bookTitle)) {
					// Found a container instead of an integer. -> Probably already uses the new data format.
					continue; // Skip
				}
				if (StringUtils.isEmpty(bookTitle)) {
					throw new InvalidDataException("Invalid book offer: Book title is empty.");
				}
				int price = offerListData.getInt(bookTitle);
				if (price <= 0) {
					throw new InvalidDataException("Invalid book offer for '" + bookTitle
							+ "': Price has to be positive, but is " + price + ".");
				}
				offers.add(new SKBookOffer(bookTitle, price));
			}
		}
		return offers;
	}
}
