package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

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
 * Wraps a given {@link CommandArgument} and makes it optional.
 * <p>
 * If no value can be parsed, no {@link ArgumentParseException} is thrown, but <code>null</code> is
 * returned instead.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public class OptionalArgument<T> extends FallbackArgument<@Nullable T> {

	// TODO Implement as 'do-nothing'-fallback instead?
	// If an invalid argument is specified, it gets currently forwarded to the next command
	// argument, leading to a likely confusing/unrelated parsing error message.
	// Fallback would jump back and throw the original parsing error if the following command
	// argument can't parse it.

	protected final CommandArgument<T> argument;

	public OptionalArgument(CommandArgument<T> argument) {
		super(Validate.notNull(argument, "argument is null").getName());
		argument.setParent(this);
		this.argument = argument;
	}

	@Override
	public boolean isOptional() {
		return true;
	}

	@Override
	public String getReducedFormat() {
		return argument.getReducedFormat();
	}

	@Override
	public Text getRequiresPlayerErrorMsg() {
		return argument.getRequiresPlayerErrorMsg();
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@FunctionalInterface
	private static interface Parser<T> {
		public T parse() throws ArgumentParseException;
	}

	private @Nullable T parseOrNull(
			Parser<T> parser,
			ArgumentsReader argsReader
	) throws FallbackArgumentException {
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		@Nullable T value;
		try {
			value = parser.parse();
		} catch (FallbackArgumentException e) {
			// Wrap fallback exceptions so that we get informed:
			throw new FallbackArgumentException(this, e);
		} catch (ArgumentParseException e) {
			// Restore previous args reader state:
			argsReader.setState(argsReaderState);
			value = null;
		}
		return value;
	}

	@Override
	public @Nullable T parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return this.parseOrNull(() -> argument.parse(input, context, argsReader), argsReader);
	}

	@Override
	public @Nullable T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		return this.parseOrNull(() -> argument.parseValue(input, context, argsReader), argsReader);
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return argument.complete(input, context, argsReader);
	}

	@Override
	public @Nullable T parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		FallbackArgumentException originalFallback = (FallbackArgumentException) fallbackException.getOriginalException();
		return this.parseOrNull(
				() -> ((FallbackArgument<T>) argument).parseFallback(
						input,
						context,
						argsReader,
						originalFallback,
						parsingFailed
				),
				argsReader
		);
	}
}
