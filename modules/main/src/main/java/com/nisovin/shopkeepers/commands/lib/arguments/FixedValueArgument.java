package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;

/**
 * A {@link CommandArgument} that returns a fixed value without consuming any arguments.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public class FixedValueArgument<T> extends CommandArgument<T> {

	private final T fixedValue; // Can be null

	public FixedValueArgument(String name, T fixedValue) {
		super(name);
		this.fixedValue = fixedValue;
	}

	@Override
	public boolean isOptional() {
		return true; // Does not require user input
	}

	@Override
	public T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return fixedValue;
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
