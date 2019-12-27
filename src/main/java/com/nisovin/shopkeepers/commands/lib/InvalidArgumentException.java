package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.text.Text;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to an invalid argument.
 */
public class InvalidArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -5970457037035687469L;

	public InvalidArgumentException(CommandArgument<?> argument, Text message) {
		this(argument, message, null);
	}

	public InvalidArgumentException(CommandArgument<?> argument, Text message, Throwable cause) {
		super(argument, message, cause);
	}
}
