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
 * A {@link PlayerFallbackArgument} that returns the sender's name if it' is a player, without consuming any arguments.
 * <p>
 * If the sender is not a player, the parsing exception of the original argument is thrown (the original argument might
 * get reevaluated is some parsing context has changed).
 */
public class SenderPlayerNameFallback extends FallbackArgument<String> {

	public static class SenderPlayerNameArgument extends CommandArgument<String> {

		public SenderPlayerNameArgument(String name) {
			super(name);
		}

		@Override
		public boolean isOptional() {
			return true; // does not require user input
		}

		@Override
		public String parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.requiresPlayerError();
			} else {
				return sender.getName();
			}
		}

		@Override
		public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
			return Collections.emptyList();
		}
	}

	public SenderPlayerNameFallback(CommandArgument<String> argument) {
		super(argument, new SenderPlayerNameArgument(Validate.notNull(argument).getName()));
	}
}
