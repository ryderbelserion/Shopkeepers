package com.nisovin.shopkeepers.commands.lib;

import java.util.List;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Command} which simply calls {@link Command#sendHelp(org.bukkit.command.CommandSender)} of the given source
 * {@link Command}.
 */
public class HelpCommand extends Command {

	private final Command helpSource;

	public HelpCommand(String name, Command helpSource) {
		this(name, null, helpSource);
	}

	public HelpCommand(String name, List<String> aliases, Command helpSource) {
		super(name, aliases);
		Validate.notNull(helpSource);
		this.helpSource = helpSource;
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		helpSource.sendHelp(input.getSender());
	}
}
