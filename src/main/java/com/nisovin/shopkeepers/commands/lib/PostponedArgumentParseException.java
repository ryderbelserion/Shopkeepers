package com.nisovin.shopkeepers.commands.lib;

/**
 *
 * An {@link ArgumentParseException} that gets thrown only if there are no other exceptions thrown during the command
 * parsing.
 * <p>
 * This can for example be used if an argument couldn't be parsed, but it expects that the remaining command arguments
 * may be able to throw a more accurate exception. This exception gets only thrown if there are no other parsing
 * exceptions.
 */
public class PostponedArgumentParseException extends ArgumentParseException {

	private static final long serialVersionUID = -2965276003159498004L;

	public PostponedArgumentParseException(String message) {
		super(message);
	}

	public PostponedArgumentParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
