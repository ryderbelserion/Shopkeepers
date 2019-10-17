package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link FallbackArgument} that returns a fixed value as fallback.
 * 
 * @see FixedValueArgument
 */
public class DefaultValueFallback<T> extends FallbackArgument<T> {

	public DefaultValueFallback(CommandArgument<T> argument, T defaultValue) {
		super(argument, new FixedValueArgument<>(Validate.notNull(argument).getName(), defaultValue));
	}
}
