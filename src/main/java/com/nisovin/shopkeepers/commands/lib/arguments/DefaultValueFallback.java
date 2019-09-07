package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;

/**
 * A {@link FallbackArgument} that returns a fixed value if none is specified.
 * <p>
 * If there are unparsed remaining arguments, the original parsing exception is thrown.
 */
public class DefaultValueFallback<T> extends FallbackArgument<T> {

	private final T defaultValue; // can be null

	public DefaultValueFallback(CommandArgument<T> argument, T defaultValue) {
		super(argument);
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean hasNoArgFallback() {
		return true;
	}

	@Override
	public T parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		if (args.hasNext()) {
			throw fallbackException.getRootException();
		} else {
			return defaultValue;
		}
	}
}
