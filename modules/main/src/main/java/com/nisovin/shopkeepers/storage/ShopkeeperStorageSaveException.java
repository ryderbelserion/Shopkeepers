package com.nisovin.shopkeepers.storage;

/**
 * Thrown when the {@link SKShopkeeperStorage} fails to save the shopkeepers data.
 */
public class ShopkeeperStorageSaveException extends Exception {

	private static final long serialVersionUID = 3348780528378613697L;

	/**
	 * Creates a new {@link ShopkeeperStorageSaveException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public ShopkeeperStorageSaveException(String message) {
		super(message);
	}

	/**
	 * Creates a new {@link ShopkeeperStorageSaveException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public ShopkeeperStorageSaveException(String message, Throwable cause) {
		super(message, cause);
	}
}
