package com.nisovin.shopkeepers.api.shopkeeper;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown when a shopkeeper fails to load its state from a given data source.
 */
public class ShopkeeperLoadException extends Exception {

	private static final long serialVersionUID = 2258914671011268570L;

	/**
	 * Creates a new {@link ShopkeeperLoadException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public ShopkeeperLoadException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new {@link ShopkeeperLoadException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public ShopkeeperLoadException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
