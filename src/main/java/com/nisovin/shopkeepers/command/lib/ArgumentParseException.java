package com.nisovin.shopkeepers.command.lib;

/**
 * This exception is thrown when an error occurs during command argument parsing, for example if an argument is of
 * invalid format.
 */
public class ArgumentParseException extends CommandException {

	private static final long serialVersionUID = -4968777515685479426L;

	public ArgumentParseException(String message) {
		super(message);
	}

	public ArgumentParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
