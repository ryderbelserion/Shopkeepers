package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link FallbackArgument} that returns the sender if it is a player, without consuming any arguments.
 * <p>
 * If the sender is not a player, a {@link RequiresPlayerArgumentException} is thrown.
 */
public class SenderPlayerFallback extends TypedFallbackArgument<Player> {

	public static class SenderPlayerArgument extends CommandArgument<Player> {

		public SenderPlayerArgument(String name) {
			super(name);
		}

		@Override
		public boolean isOptional() {
			return true; // does not require user input
		}

		@Override
		public Player parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.requiresPlayerError();
			} else {
				return (Player) sender;
			}
		}

		@Override
		public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
			return Collections.emptyList();
		}
	}

	public SenderPlayerFallback(CommandArgument<Player> argument) {
		super(argument, new SenderPlayerArgument(Validate.notNull(argument).getName()));
	}
}
