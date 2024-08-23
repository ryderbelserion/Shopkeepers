package com.nisovin.shopkeepers.commands.lib.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandSourceRejectedException;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for commands that can only be executed by players.
 */
public abstract class PlayerCommand extends Command {

	public PlayerCommand(String name) {
		this(name, Collections.emptyList());
	}

	public PlayerCommand(String name, List<String> aliases) {
		super(name, aliases);
	}

	@Override
	public boolean isAccepted(CommandSender sender) {
		return (sender instanceof Player);
	}

	@Override
	public void checkCommandSource(CommandSender sender) throws CommandSourceRejectedException {
		Validate.notNull(sender, "sender is null");
		if (!this.isAccepted(sender)) {
			throw createCommandSourceRejectedException(sender);
		}
	}

	public static CommandSourceRejectedException createCommandSourceRejectedException(
			CommandSender sender
	) {
		return new CommandSourceRejectedException(
				Text.of("You must be a player in order to execute this command!")
		);
	}
}
