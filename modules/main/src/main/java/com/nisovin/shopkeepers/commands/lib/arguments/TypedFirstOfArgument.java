package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.util.java.Pair;

/**
 * 
 * A {@link CommandArgument} similar to {@link FirstOfArgument}, but which preserves the result type of its child
 * arguments. This requires all child arguments to parse a value of the same type.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public class TypedFirstOfArgument<T> extends FallbackArgument<T> {

	// Reusing the implementation of FirstOfArgument:
	private final FirstOfArgument firstOfArgument;

	public TypedFirstOfArgument(String name, List<? extends CommandArgument<T>> arguments) {
		this(name, arguments, true, false);
	}

	public TypedFirstOfArgument(String name, List<? extends CommandArgument<T>> arguments, boolean joinFormats) {
		this(name, arguments, joinFormats, false);
	}

	public TypedFirstOfArgument(String name, List<? extends CommandArgument<T>> arguments, boolean joinFormats, boolean reverseFormat) {
		super(name);
		this.firstOfArgument = new FirstOfArgument(name + ":firstOf", arguments, joinFormats, reverseFormat);
		this.firstOfArgument.setParent(this);
	}

	@Override
	public boolean isOptional() {
		return firstOfArgument.isOptional();
	}

	@Override
	public String getReducedFormat() {
		return firstOfArgument.getReducedFormat();
	}

	@SuppressWarnings("unchecked")
	private T getValue(Pair<? extends CommandArgument<?>, ?> result) {
		return (result == null) ? null : (T) result.getSecond();
	}

	@Override
	public T parse(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		T value = this.getValue(firstOfArgument.parse(input, context, argsReader));
		if (value != null) {
			context.put(this.getName(), value);
		}
		return value;
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		return this.getValue(firstOfArgument.parseValue(input, context, argsReader));
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	@Override
	public T parseFallback(	CommandInput input, CommandContext context, ArgumentsReader argsReader,
							FallbackArgumentException fallbackException, boolean parsingFailed) throws ArgumentParseException {
		return this.getValue(firstOfArgument.parseFallback(input, context, argsReader, fallbackException, parsingFailed));
	}
}
