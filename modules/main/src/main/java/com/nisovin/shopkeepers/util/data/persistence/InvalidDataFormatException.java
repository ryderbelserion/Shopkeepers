package com.nisovin.shopkeepers.util.data.persistence;

/**
 * This exception is thrown when a {@link DataStore} cannot load its contents because the input data is in an unexpected
 * format.
 */
public class InvalidDataFormatException extends Exception {

	private static final long serialVersionUID = 599830910420908046L;

	/**
	 * Creates a new {@link InvalidDataFormatException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public InvalidDataFormatException(String message) {
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
	public InvalidDataFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
