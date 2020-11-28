package com.nisovin.shopkeepers.config.value;

/**
 * Thrown by {@link ValueType#parse(String)} if the value cannot be parsed.
 */
public class ValueParseException extends Exception {

	private static final long serialVersionUID = 4926921137692692427L;

	public ValueParseException(String message) {
		super(message);
	}

	public ValueParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
