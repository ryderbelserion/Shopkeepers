package com.nisovin.shopkeepers.util.data;

/**
 * A general-purpose exception that can be used by any component that reads data from a {@link DataContainer} to
 * indicate that there is some issue with the read data.
 */
public class InvalidDataException extends Exception {

	private static final long serialVersionUID = 7161171181992930356L;

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
