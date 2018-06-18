package com.nisovin.shopkeepers.command.lib;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;

/**
 * This exception is thrown during handling of a command when an error occurs or execution fails. The detail message of
 * the exception might get sent to the {@link CommandSender} which triggered the command.
 */
public class CommandException extends Exception {

	private static final long serialVersionUID = 3021047528891246476L;

	public CommandException(String message) {
		super(message);
		Validate.notEmpty(message, "No message specified!");
	}

	public CommandException(String message, Throwable cause) {
		super(message, cause);
		Validate.notEmpty(message, "No message specified!");
	}
}
