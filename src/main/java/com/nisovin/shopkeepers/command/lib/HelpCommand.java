package com.nisovin.shopkeepers.command.lib;

import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * A {@link Command} which simply calls {@link Command#sendHelp(org.bukkit.command.CommandSender)} of the given source
 * {@link Command}.
 */
public class HelpCommand extends Command {

	private final Command helpSource;

	public HelpCommand(Command helpSource, List<String> aliases) {
		super(aliases);
		Validate.notNull(helpSource);
		this.helpSource = helpSource;
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		helpSource.sendHelp(input.getSender());
	}
}
