package com.nisovin.shopkeepers.commands.lib;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;

/**
 * Stores info about the original command input, excluding the arguments (as those get handled separately).
 */
public final class CommandInput {

	private final CommandSender sender;
	private final String commandName;
	private final String usedAlias;

	public CommandInput(CommandSender sender, String commandName, String usedAlias) {
		Validate.notNull(sender);
		Validate.notEmpty(commandName);
		Validate.notEmpty(usedAlias);

		this.sender = sender;
		this.commandName = commandName;
		this.usedAlias = usedAlias;
	}

	/**
	 * Gets the {@link CommandSender} executing the command.
	 * 
	 * @return the command sender
	 */
	public CommandSender getSender() {
		return sender;
	}

	/**
	 * Gets the name of the executed command.
	 * 
	 * @return the name of the command
	 */
	public String getCommandName() {
		return commandName;
	}

	/**
	 * Gets the alias that was used to call the command.
	 * <p>
	 * Note: The server might allow the execution of the command with different / modified aliases (for example with the
	 * plugin's name as prefix). Therefore this might not perfectly match any of the command's aliases.
	 * 
	 * @return the used alias
	 */
	public String getUsedAlias() {
		return usedAlias;
	}
}
