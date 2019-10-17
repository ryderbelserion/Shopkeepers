package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link FallbackArgument} that returns the sender if it is a player, without consuming any arguments.
 * <p>
 * If the sender is not a player, the parsing exception of the original argument is thrown (the original argument might
 * get reevaluated is some parsing context has changed).
 */
public class SenderPlayerFallback extends FallbackArgument<Player> {

	public static class SenderPlayerArgument extends CommandArgument<Player> {

		public SenderPlayerArgument(String name) {
			super(name);
		}

		@Override
		public boolean isOptional() {
			return true; // does not require user input
		}

		@Override
		public Player parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.requiresPlayerError();
			} else {
				return (Player) sender;
			}
		}

		@Override
		public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
			return Collections.emptyList();
		}
	}

	public SenderPlayerFallback(CommandArgument<Player> argument) {
		super(argument, new SenderPlayerArgument(Validate.notNull(argument).getName()));
	}
}
