package com.nisovin.shopkeepers.storage;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;

/**
 * Thrown when the {@link ShopkeeperStorage} fails to save the data of a shopkeeper.
 */
public class ShopkeeperStorageSaveException extends Exception {

	private static final long serialVersionUID = 3348780528378613697L;

	/**
	 * Creates a new {@link ShopkeeperStorageSaveException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public ShopkeeperStorageSaveException(@Nullable String message) {
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
	public ShopkeeperStorageSaveException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
