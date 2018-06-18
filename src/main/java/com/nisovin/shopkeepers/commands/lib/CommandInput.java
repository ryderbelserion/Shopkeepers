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

	public CommandSender getSender() {
		return sender;
	}

	public String getCommandName() {
		return commandName;
	}

	public String getUsedAlias() {
		return usedAlias;
	}
}
