package com.nisovin.shopkeepers.property;

/**
 * An exception that indicates as issue when a {@link Property} loads its value.
 */
public class InvalidValueException extends Exception {

	private static final long serialVersionUID = 3489960062419292733L;

	/**
	 * Creates a new {@link InvalidValueException}.
	 * 
	 * @param message
	 *            the exception message
	 */
	public InvalidValueException(String message) {
		super(message);
	}
}
