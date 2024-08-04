package com.nisovin.shopkeepers.commands.lib.commands;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link Command} which simply calls {@link Command#sendHelp(org.bukkit.command.CommandSender)}
 * of the given source {@link Command}.
 */
public class HelpCommand extends Command {

	private final Command helpSource;

	public HelpCommand(String name, Command helpSource) {
		this(name, Collections.emptyList(), helpSource);
	}

	public HelpCommand(String name, List<String> aliases, Command helpSource) {
		super(name, aliases);
		Validate.notNull(helpSource, "helpSource is null");
		this.helpSource = helpSource;
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		helpSource.sendHelp(input.getSender());
	}
}
