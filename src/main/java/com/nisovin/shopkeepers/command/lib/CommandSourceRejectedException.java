package com.nisovin.shopkeepers.command.lib;

/**
 * This exception is thrown when a command cannot be executed because the command source is not accepted.
 */
public class CommandSourceRejectedException extends CommandException {

	private static final long serialVersionUID = -3136267542969449218L;

	public CommandSourceRejectedException(String message) {
		super(message);
	}

	public CommandSourceRejectedException(String message, Throwable cause) {
		super(message, cause);
	}
}
