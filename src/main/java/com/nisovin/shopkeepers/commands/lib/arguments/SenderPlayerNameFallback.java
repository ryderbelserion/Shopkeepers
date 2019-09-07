package com.nisovin.shopkeepers.commands.lib.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;

/**
 * A {@link FallbackArgument} that returns the sender's name if it's a player and if there are no arguments to parse.
 * <p>
 * If there are unparsed remaining arguments, the original parsing exception is thrown.
 * <p>
 * If the sender is no player, a 'missing argument' exception is thrown (since it is assumed that this is used as
 * fallback for a player name argument).
 */
public class SenderPlayerNameFallback extends FallbackArgument<String> {

	public SenderPlayerNameFallback(CommandArgument<String> argument) {
		super(argument);
	}

	@Override
	public boolean hasNoArgFallback() {
		return true;
	}

	@Override
	public String parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		String arg = args.nextIfPresent();
		if (arg != null) {
			throw fallbackException.getRootException();
		} else {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.missingArgument();
			} else {
				return sender.getName();
			}
		}
	}
}
