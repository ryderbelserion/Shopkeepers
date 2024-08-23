package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.ObjectUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that returns the sender if it is a player, without consuming any
 * arguments.
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
			return true; // Does not require user input
		}

		@Override
		public Player parseValue(
				CommandInput input,
				CommandContextView context,
				ArgumentsReader argsReader
		) throws ArgumentParseException {
			Player player = ObjectUtils.castOrNull(input.getSender(), Player.class);
			if (player == null) {
				throw this.requiresPlayerError();
			} else {
				return player;
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

	public SenderPlayerFallback(CommandArgument<Player> argument) {
		super(
				Validate.notNull(argument, "argument is null"),
				new SenderPlayerArgument(argument.getName())
		);
	}
}
