package com.nisovin.shopkeepers.util.data.persistence;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown when a {@link DataStore} cannot load its contents because the input data
 * is in an unexpected format.
 */
public class InvalidDataFormatException extends Exception {

	private static final long serialVersionUID = 599830910420908046L;

	/**
	 * Creates a new {@link InvalidDataFormatException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public InvalidDataFormatException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new {@link InvalidDataFormatException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public InvalidDataFormatException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
