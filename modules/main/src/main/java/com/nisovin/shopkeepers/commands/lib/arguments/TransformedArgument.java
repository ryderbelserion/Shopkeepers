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
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Wraps another {@link CommandArgument} and transforms its result value.
 * 
 * @param <T>
 *            the original value type
 * @param <R>
 *            the transformed value type
 */
public class TransformedArgument<T, R> extends FallbackArgument<R> {

	@FunctionalInterface
	public interface ArgumentTransformer<T, R> {
		/**
		 * Transforms one value into another.
		 * 
		 * @param input
		 *            the input value
		 * @return the transformed value
		 * @throws ArgumentParseException
		 *             in case the input cannot be transformed for some reason, but
		 *             {@link FallbackArgumentException} is not allowed
		 */
		public R apply(T input) throws ArgumentParseException;
	}

	private final CommandArgument<T> fromArgument;
	private final ArgumentTransformer<T, R> transformer;

	public TransformedArgument(
			CommandArgument<T> fromArgument,
			ArgumentTransformer<T, R> transformer
	) {
		super(Validate.notNull(fromArgument, "fromArgument is null").getName());
		Validate.notNull(transformer, "transformer is null");
		this.fromArgument = fromArgument;
		fromArgument.setParent(this);
		this.transformer = transformer;
	}

	@Override
	public R parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		T fromValue;
		try {
			fromValue = fromArgument.parseValue(input, context, argsReader);
		} catch (FallbackArgumentException e) {
			// Wrap and re-throw so that we get informed on fallback evaluation:
			throw new FallbackArgumentException(this, e);
		}
		return transformer.apply(fromValue);
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return fromArgument.complete(input, context, argsReader);
	}

	@Override
	public R parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		FallbackArgumentException originalFallback = (FallbackArgumentException) fallbackException.getOriginalException();
		T fromValue = ((FallbackArgument<T>) fromArgument).parseFallback(
				input,
				context,
				argsReader,
				originalFallback,
				parsingFailed
		);
		return transformer.apply(fromValue);
	}
}
