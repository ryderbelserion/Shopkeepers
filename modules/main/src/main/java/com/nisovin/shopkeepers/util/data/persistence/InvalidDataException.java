package com.nisovin.shopkeepers.util.data.persistence;

/**
 * This exception is thrown by {@link DataStore}s when the data container contents cannot be reconstructed because the
 * input data is not in a supported format.
 */
public class InvalidDataException extends Exception {

	private static final long serialVersionUID = 599830910420908046L;

	/**
	 * Creates a new {@link InvalidDataException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public InvalidDataException(String message) {
		super(message);
	}

	/**
	 * Creates a new {@link InvalidDataException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public InvalidDataException(String message, Throwable cause) {
		super(message, cause);
	}
}
