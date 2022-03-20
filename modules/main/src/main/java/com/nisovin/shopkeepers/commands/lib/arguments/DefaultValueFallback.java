package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that returns a fixed value as fallback.
 * 
 * @param <T>
 *            the type of the parsed argument
 * @see FixedValueArgument
 */
public class DefaultValueFallback<T> extends TypedFallbackArgument<T> {

	public DefaultValueFallback(CommandArgument<T> argument, T defaultValue) {
		super(
				Validate.notNull(argument, "argument is null"),
				new FixedValueArgument<>(argument.getName(), defaultValue)
		);
	}
}
