package com.nisovin.shopkeepers.util.data;

/**
 * A specialization of {@link InvalidDataException} for cases in which some required data is missing.
 */
public class MissingDataException extends InvalidDataException {

	private static final long serialVersionUID = 7542690388367072712L;

	/**
	 * Creates a new {@link MissingDataException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public MissingDataException(String message) {
		super(message);
	}

	/**
	 * Creates a new {@link MissingDataException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public MissingDataException(String message, Throwable cause) {
		super(message, cause);
	}
}
