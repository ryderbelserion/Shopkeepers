package com.nisovin.shopkeepers.api.shopkeeper;

/**
 * This exception gets used during shopkeeper creation and loading, if the shopkeeper cannot be created due to invalid
 * or missing data.
 */
public class ShopkeeperCreateException extends Exception {

	private static final long serialVersionUID = -2026963951805397944L;

	/**
	 * Creates a new {@link ShopkeeperCreateException}.
	 * 
	 * @param message
	 *            the exception message
	 */
	public ShopkeeperCreateException(String message) {
		super(message);
	}
}
