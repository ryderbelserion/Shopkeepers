package com.nisovin.shopkeepers.api.shopkeeper.offers;

import com.nisovin.shopkeepers.api.internal.ApiInternals;

/**
 * Stores information about a certain book being traded for a certain price.
 * <p>
 * Books are identified solely based on their title.
 * <p>
 * Instances of this are immutable. They can be created via {@link #create(String, int)}.
 */
public interface BookOffer {

	/**
	 * Creates a new {@link BookOffer}.
	 * 
	 * @param bookTitle
	 *            the book title, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static BookOffer create(String bookTitle, int price) {
		return ApiInternals.getInstance().createBookOffer(bookTitle, price);
	}

	// ----

	/**
	 * Gets the title of the book being traded.
	 * 
	 * @return the book title, not <code>null</code> or empty
	 */
	public String getBookTitle();

	/**
	 * Gets the price.
	 * 
	 * @return the price, a positive number
	 */
	public int getPrice();
}
