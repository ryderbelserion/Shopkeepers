package com.nisovin.shopkeepers.commands.lib;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to an invalid argument.
 */
public class InvalidArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -5970457037035687469L;

	public InvalidArgumentException(String message) {
		super(message);
	}

	public InvalidArgumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
