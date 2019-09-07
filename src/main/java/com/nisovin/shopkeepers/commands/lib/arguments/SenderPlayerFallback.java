package com.nisovin.shopkeepers.commands.lib.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;

/**
 * A {@link FallbackArgument} that returns the sender if it's a player and if there are no arguments to parse.
 * <p>
 * If there are unparsed remaining arguments, the original parsing exception is thrown.
 * <p>
 * If the sender is no player, a 'missing argument' exception is thrown (since it is assumed that this is used as
 * fallback for a player argument).
 */
public class SenderPlayerFallback extends FallbackArgument<Player> {

	public SenderPlayerFallback(CommandArgument<Player> argument) {
		super(argument);
	}

	@Override
	public boolean hasNoArgFallback() {
		return true;
	}

	@Override
	public Player parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		String arg = args.nextIfPresent();
		if (arg != null) {
			throw fallbackException.getRootException();
		} else {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.missingArgument();
			} else {
				return (Player) sender;
			}
		}
	}
}
