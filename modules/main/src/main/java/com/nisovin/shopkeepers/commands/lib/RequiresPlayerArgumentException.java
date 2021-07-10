package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.text.Text;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to the argument requiring a player to
 * execute the command.
 */
public class RequiresPlayerArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = 8158065171648348988L;

	public RequiresPlayerArgumentException(CommandArgument<?> argument, Text message) {
		this(argument, message, null);
	}

	public RequiresPlayerArgumentException(CommandArgument<?> argument, Text message, Throwable cause) {
		super(argument, message, cause);
	}
}
