package com.nisovin.shopkeepers.commands.lib;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to there being no argument to parse.
 */
public class MissingArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -3269722516077651284L;

	public MissingArgumentException(CommandArgument<?> argument, String message) {
		this(argument, message, null);
	}

	public MissingArgumentException(CommandArgument<?> argument, String message, Throwable cause) {
		super(argument, message, cause);
	}
}
