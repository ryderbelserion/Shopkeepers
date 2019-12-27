package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.text.Text;

/**
 * This exception is thrown when an error occurs during command argument parsing, for example if an argument is of
 * invalid format.
 */
public class ArgumentParseException extends CommandException {

	private static final long serialVersionUID = -4968777515685479426L;

	private final CommandArgument<?> argument; // can be null if not caused by any CommandArgument

	public ArgumentParseException(CommandArgument<?> argument, Text message) {
		this(argument, message, null);
	}

	public ArgumentParseException(CommandArgument<?> argument, Text message, Throwable cause) {
		super(message, cause);
		this.argument = argument;
	}

	/**
	 * Gets the {@link CommandArgument} that created this exception.
	 * 
	 * @return the command argument, or <code>null</code> if this exception was not created by any command argument (but
	 *         for example by the parsing {@link Command} itself)
	 */
	public CommandArgument<?> getArgument() {
		return argument;
	}
}
