package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that wraps two {@link CommandArgument command arguments}: If parsing
 * the first command argument fails, the second command argument gets evaluated as fallback.
 * <p>
 * Unlike {@link AnyFallbackArgument} this provides a value of a specific type. This requires the
 * both original and the fallback argument to be of the same type.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public class TypedFallbackArgument<T> extends FallbackArgument<T> {

	// Reusing the implementation of AnyFallbackArgument:
	private final AnyFallbackArgument anyFallbackArgument;

	public TypedFallbackArgument(CommandArgument<T> argument, CommandArgument<T> fallbackArgument) {
		super(Validate.notNull(argument, "argument is null").getName());
		this.anyFallbackArgument = new AnyFallbackArgument(argument, fallbackArgument);
		this.anyFallbackArgument.setParent(this);
	}

	@SuppressWarnings("unchecked")
	public CommandArgument<T> getOriginalArgument() {
		return (CommandArgument<T>) anyFallbackArgument.getOriginalArgument();
	}

	@SuppressWarnings("unchecked")
	public CommandArgument<T> getFallbackArgument() {
		return (CommandArgument<T>) anyFallbackArgument.getFallbackArgument();
	}

	@Override
	public String getReducedFormat() {
		return anyFallbackArgument.getReducedFormat();
	}

	@Override
	public boolean isOptional() {
		return anyFallbackArgument.isOptional();
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		return anyFallbackArgument.getMissingArgumentErrorMsg();
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		return anyFallbackArgument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return (T) anyFallbackArgument.parse(input, context, argsReader);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return (T) anyFallbackArgument.parseValue(input, context, argsReader);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		return (T) anyFallbackArgument.parseFallback(
				input,
				context,
				argsReader,
				fallbackException,
				parsingFailed
		);
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return anyFallbackArgument.complete(input, context, argsReader);
	}
}
