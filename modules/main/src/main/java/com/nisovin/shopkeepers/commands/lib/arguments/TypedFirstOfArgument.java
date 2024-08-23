package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.Pair;

/**
 * 
 * A {@link CommandArgument} similar to {@link FirstOfArgument}, but which preserves the result type
 * of its child arguments. This requires all child arguments to parse a value of the same type.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public class TypedFirstOfArgument<T> extends FallbackArgument<T> {

	// Reusing the implementation of FirstOfArgument:
	private final FirstOfArgument firstOfArgument;

	public TypedFirstOfArgument(
			String name,
			List<? extends CommandArgument<T>> arguments
	) {
		this(name, arguments, true, false);
	}

	public TypedFirstOfArgument(
			String name,
			List<? extends CommandArgument<T>> arguments,
			boolean joinFormats
	) {
		this(name, arguments, joinFormats, false);
	}

	public TypedFirstOfArgument(
			String name,
			List<? extends CommandArgument<T>> arguments,
			boolean joinFormats,
			boolean reverseFormat
	) {
		super(name);
		this.firstOfArgument = new FirstOfArgument(
				name + ":firstOf",
				arguments,
				joinFormats,
				reverseFormat
		);
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

	// This is expected to only return null if T is nullable.
	@SuppressWarnings("unchecked")
	private T getValue(@Nullable Pair<? extends CommandArgument<?>, ?> result) {
		return (result != null) ? (T) result.getSecond() : Unsafe.uncheckedNull();
	}

	@Override
	public T parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		T value = this.getValue(firstOfArgument.parse(input, context, argsReader));
		// TODO Separate variable is required due to a Checker Framework false positive, caused by
		// the if condition
		T result = value;
		if (value != null) {
			context.put(this.getName(), value);
		}
		return result;
	}

	@Override
	public T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return this.getValue(firstOfArgument.parseValue(input, context, argsReader));
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	@Override
	public T parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		return this.getValue(firstOfArgument.parseFallback(
				input,
				context,
				argsReader,
				fallbackException,
				parsingFailed
		));
	}
}
