package com.nisovin.shopkeepers.commands.lib;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to the argument requiring a player to
 * execute the command.
 */
public class RequiresPlayerArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = 8158065171648348988L;

	public RequiresPlayerArgumentException(String message) {
		super(message);
	}

	public RequiresPlayerArgumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
