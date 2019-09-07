package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;

/**
 * A {@link FallbackArgument} that accepts any String as fallback.
 */
public class AnyStringFallback extends FallbackArgument<String> {

	private final boolean joinRemainingArgs;

	public AnyStringFallback(CommandArgument<String> argument) {
		this(argument, false);
	}

	public AnyStringFallback(CommandArgument<String> argument, boolean joinRemainingArgs) {
		super(argument);
		this.joinRemainingArgs = joinRemainingArgs;
	}

	@Override
	public boolean hasNoArgFallback() {
		return false;
	}

	@Override
	public String parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		} else {
			if (joinRemainingArgs) {
				return StringArgument.getJoinedRemainingArgs(args);
			} else {
				return args.next();
			}
		}
	}
}
