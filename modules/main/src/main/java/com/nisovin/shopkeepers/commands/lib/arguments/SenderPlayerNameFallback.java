package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that returns the sender's name if it is a player, without consuming
 * any arguments.
 * <p>
 * If the sender is not a player, a {@link RequiresPlayerArgumentException} is thrown.
 */
public class SenderPlayerNameFallback extends TypedFallbackArgument<String> {

	public static class SenderPlayerNameArgument extends CommandArgument<String> {

		public SenderPlayerNameArgument(String name) {
			super(name);
		}

		@Override
		public boolean isOptional() {
			return true; // Does not require user input
		}

		@Override
		public String parseValue(
				CommandInput input,
				CommandContextView context,
				ArgumentsReader argsReader
		) throws ArgumentParseException {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.requiresPlayerError();
			} else {
				return sender.getName();
			}
		}

		@Override
		public List<? extends String> complete(
				CommandInput input,
				CommandContextView context,
				ArgumentsReader argsReader
		) {
			return Collections.emptyList();
		}
	}

	public SenderPlayerNameFallback(CommandArgument<String> argument) {
		super(
				Validate.notNull(argument, "argument is null"),
				new SenderPlayerNameArgument(argument.getName())
		);
	}
}
