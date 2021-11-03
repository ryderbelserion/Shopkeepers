package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DataValue;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
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

	private static final Property<String> BOOK_TITLE = new BasicProperty<String>()
			.dataKeyAccessor("book", StringSerializers.SCALAR)
			.validator(StringValidators.NON_EMPTY)
			.build();
	private static final Property<Integer> PRICE = new BasicProperty<Integer>()
			.dataKeyAccessor("price", NumberSerializers.INTEGER)
			.validator(IntegerValidators.POSITIVE)
			.build();

	/**
	 * A {@link DataSerializer} for values of type {@link BookOffer}.
	 */
	public static final DataSerializer<BookOffer> SERIALIZER = new DataSerializer<BookOffer>() {
		@Override
		public Object serialize(BookOffer value) {
			Validate.notNull(value, "value is null");
			DataContainer offerData = DataContainer.create();
			offerData.set(BOOK_TITLE, value.getBookTitle());
			offerData.set(PRICE, value.getPrice());
			return offerData.serialize();
		}

		@Override
		public BookOffer deserialize(Object data) throws InvalidDataException {
			DataContainer offerData = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				String bookTitle = offerData.get(BOOK_TITLE);
				int price = offerData.get(PRICE);
				return new SKBookOffer(bookTitle, price);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link BookOffer}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<@ReadOnly List<? extends BookOffer>> LIST_SERIALIZER = new DataSerializer<List<? extends BookOffer>>() {
		@Override
		public Object serialize(List<? extends BookOffer> value) {
			Validate.notNull(value, "value is null");
			DataContainer offerListData = DataContainer.create();
			int id = 1;
			for (BookOffer offer : value) {
				Validate.notNull(offer, "list of offers contains null");
				offerListData.set(String.valueOf(id), SERIALIZER.serialize(offer));
				id++;
			}
			return offerListData.serialize();
		}

		@Override
		public List<? extends BookOffer> deserialize(Object data) throws InvalidDataException {
			DataContainer offerListData = DataContainerSerializers.DEFAULT.deserialize(data);
			Set<String> keys = offerListData.getKeys();
			List<BookOffer> offers = new ArrayList<>(keys.size());
			for (String id : keys) {
				Object offerData = offerListData.get(id);
				BookOffer offer;
				try {
					offer = SERIALIZER.deserialize(offerData);
				} catch (InvalidDataException e) {
					throw new InvalidDataException("Invalid book offer " + id + ": " + e.getMessage(), e);
				}
				offers.add(offer);
			}
			return offers;
		}
	};

	public static void saveOffers(DataValue dataValue, List<? extends BookOffer> offers) {
		Validate.notNull(dataValue, "dataValue is null");
		if (offers == null) {
			dataValue.clear();
			return;
		}

		Object offerListData = LIST_SERIALIZER.serialize(offers);
		dataValue.set(offerListData);
	}

	public static List<? extends BookOffer> loadOffers(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");

		if (!dataValue.isPresent()) {
			// No data. -> Return an empty list of offers.
			return Collections.emptyList();
		}

		Object offerListData = dataValue.get();
		return LIST_SERIALIZER.deserialize(offerListData);
	}

	// Returns true if the data has changed due to migrations.
	public static boolean migrateOffers(DataValue dataValue, String logPrefix) throws InvalidDataException {
		Validate.notNull(logPrefix, "logPrefix is null");
		// TODO Remove this migration again at some point (added in late MC 1.14.4).
		List<? extends BookOffer> legacyOffers = loadLegacyOffers(dataValue);
		if (legacyOffers.isEmpty()) {
			// Nothing to migrate.
			return false;
		}

		// Assertion: We do not expect there to be a mix of legacy and non-legacy offers. We can therefore skip loading
		// any non-legacy offers.
		// Write back the migrated offers:
		saveOffers(dataValue, legacyOffers);
		Log.info(logPrefix + "Migrated old book offers.");
		return true;
	}

	// Legacy format: bookTitle -> price mapping
	private static List<? extends BookOffer> loadLegacyOffers(DataValue dataValue) throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");
		DataContainer offerListData = dataValue.getContainer();
		if (offerListData == null) {
			return Collections.emptyList();
		}

		List<BookOffer> offers = new ArrayList<>();
		for (String bookTitle : offerListData.getKeys()) {
			if (offerListData.isContainer(bookTitle)) {
				// Found a container instead of an integer. -> We assume that the offers have already been migrated to
				// the new data format, and therefore abort the loading of legacy book offers.
				if (!offers.isEmpty()) {
					throw new InvalidDataException("Found a mix of legacy and non-legacy book offers!");
				}
				return Collections.emptyList(); // Abort
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
		return offers;
	}
}
