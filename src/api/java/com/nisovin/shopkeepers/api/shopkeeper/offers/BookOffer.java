package com.nisovin.shopkeepers.api.shopkeeper.offers;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;

/**
 * Stores information about a certain book being traded for a certain price.
 * <p>
 * Books are identified solely based on their title.
 * <p>
 * Instances of this can be created via {@link ShopkeepersAPI#createBookOffer(String, int)}.
 */
public interface BookOffer {

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
