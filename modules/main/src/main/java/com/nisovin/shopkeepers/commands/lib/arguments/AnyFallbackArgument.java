package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.MissingArgumentException;
import com.nisovin.shopkeepers.commands.lib.argument.RequiresPlayerArgumentException;
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
 * {@link FallbackArgument}s can be chained. When the fallback gets evaluated it checks for and
 * evaluates chained child fallbacks first. Since the fallback cannot throw another
 * {@link FallbackArgumentException}, the actual fallback argument is not allowed to be a
 * {@link FallbackArgument} itself.
 * <p>
 * The original and the fallback argument may provide values of different types. If they both
 * provide a value of the same type, {@link TypedFallbackArgument} can be used to preserve that
 * type.
 */
public class AnyFallbackArgument extends FallbackArgument<@Nullable Object> {

	protected final CommandArgument<?> argument; // This may be a fallback argument itself
	protected final CommandArgument<?> fallbackArgument;

	public AnyFallbackArgument(CommandArgument<?> argument, CommandArgument<?> fallbackArgument) {
		super(Validate.notNull(argument, "argument is null").getName());
		Validate.notNull(fallbackArgument, "fallbackArgument is null");
		Validate.isTrue(!(fallbackArgument instanceof FallbackArgument),
				"fallbackArgument cannot be a FallbackArgument itself");
		this.argument = argument;
		this.fallbackArgument = fallbackArgument;

		argument.setParent(this);
		fallbackArgument.setParent(this);
	}

	// May itself by a FallbackArgument.
	public CommandArgument<?> getOriginalArgument() {
		return argument;
	}

	public CommandArgument<?> getFallbackArgument() {
		return fallbackArgument;
	}

	@Override
	public String getReducedFormat() {
		return argument.getReducedFormat();
	}

	@Override
	public boolean isOptional() {
		return argument.isOptional() || fallbackArgument.isOptional();
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@Override
	public @Nullable Object parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		try {
			return argument.parse(input, context, argsReader);
		} catch (ArgumentParseException e) {
			// Note: Caller is responsible for resetting the args reader if required.
			throw new FallbackArgumentException(this, e); // Throw fallback exception
		}
	}

	@Override
	public @Nullable Object parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		try {
			return argument.parseValue(input, context, argsReader);
		} catch (ArgumentParseException e) {
			// Note: Caller is responsible for resetting the args reader if required.
			throw new FallbackArgumentException(this, e); // Throw fallback exception
		}
	}

	// parsingFailed: Whether parsing the following command arguments failed. The ArgumentsReader
	// got reset to the original state in that case. Otherwise, if parsing succeeded, the
	// ArgumentsReader will have no remaining unparsed arguments.
	@Override
	public @Nullable Object parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		// Fallback chaining: If the original exception was a fallback itself, try it first.
		// TODO Unchecked cast: CheckerFramework complains when using a wildcard here.
		Optional<@Nullable Object> originalFallbackValue = Unsafe.cast(this.parseOriginalFallback(
				input,
				context,
				argsReader,
				fallbackException,
				parsingFailed
		));
		if (originalFallbackValue != null) {
			// TODO Unchecked null due to CheckerFramework false positive
			return originalFallbackValue.orElse(null);
		}

		// Try to parse fallback argument:
		try {
			return fallbackArgument.parse(input, context, argsReader);
		} catch (MissingArgumentException | RequiresPlayerArgumentException e) {
			// If the fallback throws a 'missing argument' or 'requires player' exception, prefer
			// the parsing error of the original argument
			// (TODO only if the original error has been / is (after re-evaluating it) a 'missing
			// argument' or 'requires player' error as well?):
			if (parsingFailed) {
				// The ArgumentsReader got reset, so re-evaluating the original command arguments is
				// not expected to be required / to yield a different outcome:
				throw fallbackException.getRootException();
			} else {
				// Parsing past the fallback argument succeeded, so the original argument was likely
				// in a different situation before than it is the case now. Re-evaluate the original
				// argument will therefore likely yield a different error now (likely 'missing
				// argument' or 'requires players').
				// We can't however be sure that this is the case since we don't know the nature /
				// internals of the original argument. It might for example be an argument that
				// doesn't consume any arguments. We therefore cannot simply throw a 'missing
				// argument'/'requires player' exception in its name here.
				try {
					return argument.parse(input, context, argsReader);
					// Unexpected, but in case the original argument succeeds now, use the parsed
					// value.
				} catch (FallbackArgumentException fe) {
					// In case we are inside a chain of fallbacks, use the original (root)
					// exception:
					throw fe.getRootException();
				} // Forward any other type of parsing exception.
			}
		}
	}

	// Returns null if no original fallback was able to parse a fallback value
	@SuppressWarnings("annotations.on.use")
	protected final @Nullable Optional<?> parseOriginalFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) {
		ArgumentParseException originalException = fallbackException.getOriginalException();
		if (originalException instanceof FallbackArgumentException) {
			FallbackArgumentException originalFallback = (FallbackArgumentException) originalException;
			FallbackArgument<?> originalFallbackArgument = originalFallback.getArgument();
			try {
				// If the original fallback succeeds, skip our fallback:
				// The result may be empty (null).
				// The original fallback is expected to be of the same type.
				return Optional.ofNullable(originalFallbackArgument.parseFallback(
						input,
						context,
						argsReader,
						originalFallback,
						parsingFailed
				));
			} catch (FallbackArgumentException e) {
				// Fallback is not allowed to throw another fallback exception here.
				Validate.State.error("Original fallback argument '"
						+ originalFallbackArgument.getName()
						+ "' threw another FallbackArgumentException while parsing fallback: " + e);
			} catch (ArgumentParseException e) {
				// The original fallback failed: Ignore and return null.
			}
		}
		return null;
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		// Combine suggestions of the original and the fallback argument:
		// Keep track of the initial state:
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		List<? extends String> argumentSuggestions = argument.complete(
				input,
				context,
				argsReader
		);
		if (argumentSuggestions.size() >= MAX_SUGGESTIONS) return argumentSuggestions;

		List<String> suggestions = new ArrayList<>(argumentSuggestions);
		int limit = (MAX_SUGGESTIONS - suggestions.size());
		assert limit > 0;

		// Reset args so that the fallback argument has a chance to provide different completions:
		argsReader.setState(argsReaderState);
		List<? extends String> fallbackSuggestions = fallbackArgument.complete(
				input,
				context,
				argsReader
		);

		if (fallbackSuggestions.size() <= limit) {
			suggestions.addAll(fallbackSuggestions);
		} else {
			suggestions.addAll(fallbackSuggestions.subList(0, limit));
		}
		return Collections.unmodifiableList(suggestions);
	}
}
