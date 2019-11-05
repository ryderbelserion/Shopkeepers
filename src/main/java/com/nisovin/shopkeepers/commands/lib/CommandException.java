package com.nisovin.shopkeepers.commands.lib;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.util.Validate;

/**
 * This exception is thrown during handling of a command when an error occurs or execution fails. The detail message of
 * the exception might get sent to the {@link CommandSender} which triggered the command.
 */
public class CommandException extends Exception {

	private static final long serialVersionUID = 3021047528891246476L;

	public CommandException(String message) {
		this(message, null);
	}

	public CommandException(String message, Throwable cause) {
		super(message, cause);
		Validate.notEmpty(message, "No message specified!");
	}
}
