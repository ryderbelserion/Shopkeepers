package com.nisovin.shopkeepers.command.shopkeepers;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.command.lib.Command;
import com.nisovin.shopkeepers.command.lib.CommandSourceRejectedException;

/**
 * Base class for commands that can only be executed by players.
 */
abstract class PlayerCommand extends Command {

	PlayerCommand(List<String> aliases) {
		super(aliases);
	}

	@Override
	public boolean isAccepted(CommandSender sender) {
		return (sender instanceof Player);
	}

	@Override
	public void checkCommandSource(CommandSender sender) throws CommandSourceRejectedException {
		Validate.notNull(sender);
		if (!this.isAccepted(sender)) {
			throw createCommandSourceRejectedException(sender);
		}
	}

	public static CommandSourceRejectedException createCommandSourceRejectedException(CommandSender sender) {
		return new CommandSourceRejectedException("You must be a player in order to execute this command!");
	}
}
