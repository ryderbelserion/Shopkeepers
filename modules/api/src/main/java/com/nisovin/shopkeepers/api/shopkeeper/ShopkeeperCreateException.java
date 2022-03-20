package com.nisovin.shopkeepers.api.shopkeeper;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown when the {@link ShopkeeperRegistry#createShopkeeper(ShopCreationData)
 * creation} of a new shopkeeper fails for some reason.
 */
public class ShopkeeperCreateException extends Exception {

	private static final long serialVersionUID = -2026963951805397944L;

	/**
	 * Creates a new {@link ShopkeeperCreateException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public ShopkeeperCreateException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new {@link ShopkeeperCreateException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public ShopkeeperCreateException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
