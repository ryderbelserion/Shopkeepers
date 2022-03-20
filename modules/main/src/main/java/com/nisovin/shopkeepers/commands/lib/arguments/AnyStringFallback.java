package com.nisovin.shopkeepers.commands.lib.arguments;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that accepts any String as fallback.
 * 
 * @see StringArgument
 */
public class AnyStringFallback extends TypedFallbackArgument<@NonNull String> {

	public AnyStringFallback(CommandArgument<@NonNull String> argument) {
		this(argument, false);
	}

	public AnyStringFallback(CommandArgument<@NonNull String> argument, boolean joinRemainingArgs) {
		super(
				Validate.notNull(argument, "argument is null"),
				new StringArgument(argument.getName(), joinRemainingArgs)
		);
	}
}
