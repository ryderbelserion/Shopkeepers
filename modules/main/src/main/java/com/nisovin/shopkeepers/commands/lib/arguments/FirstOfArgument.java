package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.Pair;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link CommandArgument} which stores the value for the first of the given command arguments
 * which successfully parses something.
 * <p>
 * If an argument provides <code>null</code> as value, parsing continues, giving the remaining
 * arguments a chance to parse something 'more useful'. However, no {@link ArgumentParseException}
 * will be thrown in this case if no argument parses a non-<code>null</code> value.
 */
public class FirstOfArgument
		extends FallbackArgument<@Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?>> {
	// Extending FallbackArgument because we might forward FallbackArgumentExceptions of our child
	// arguments.

	public static final String FORMAT_DELIMITER = "|";

	private final List<@NonNull CommandArgument<?>> arguments;
	// Null if the default (parent) reduced format shall be used:
	private final @Nullable String reducedFormat;

	public FirstOfArgument(String name, List<? extends @NonNull CommandArgument<?>> arguments) {
		this(name, arguments, true, false);
	}

	public FirstOfArgument(
			String name,
			List<? extends @NonNull CommandArgument<?>> arguments,
			boolean joinFormats
	) {
		this(name, arguments, joinFormats, false);
	}

	public FirstOfArgument(
			String name,
			List<? extends @NonNull CommandArgument<?>> arguments,
			boolean joinFormats,
			boolean reverseFormat
	) {
		super(name);

		// Arguments:
		Validate.notNull(arguments, "arguments is null");
		Validate.isTrue(!arguments.isEmpty(), "arguments is empty");
		List<@NonNull CommandArgument<?>> argumentsList = new ArrayList<>(arguments.size());
		this.arguments = Collections.unmodifiableList(argumentsList);
		for (CommandArgument<?> argument : arguments) {
			Validate.notNull(argument, "arguments contains null");
			argument.setParent(this);
			argumentsList.add(argument);
		}
		assert !this.arguments.isEmpty();

		// Format:
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			ListIterator<? extends @NonNull CommandArgument<?>> iterator = this.arguments.listIterator(reverseFormat ? this.arguments.size() : 0);
			while (reverseFormat ? iterator.hasPrevious() : iterator.hasNext()) {
				@Nullable CommandArgument<?> argument = (reverseFormat ? iterator.previous() : iterator.next());
				assert argument != null;
				// Appending reduced format for child-arguments here:
				String argumentFormat = argument.getReducedFormat();
				if (!argumentFormat.isEmpty()) {
					format.append(argumentFormat).append(delimiter);
				}
			}
			if (format.length() == 0) {
				this.reducedFormat = "";
			} else {
				this.reducedFormat = format.substring(0, format.length() - delimiter.length());
			}
		} else {
			// Using the default format:
			this.reducedFormat = null;
		}
	}

	@Override
	public boolean isOptional() {
		// This argument is optional, if at least one child-argument is optional:
		for (CommandArgument<?> argument : arguments) {
			if (argument.isOptional()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getReducedFormat() {
		if (reducedFormat != null) {
			return reducedFormat;
		} else {
			return super.getReducedFormat();
		}
	}

	@FunctionalInterface
	private static interface Parser<@NonNull I> {
		public @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> parse(
				@NonNull I input
		) throws ArgumentParseException;
	}

	// Returns a pair with the argument and the parsed value, or null if nothing was parsed.
	private <@NonNull I> @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> parseFirstOf(
			Iterable<? extends @NonNull I> inputs,
			Parser<@NonNull I> parser,
			ArgumentsReader argsReader,
			boolean parsingFallbacks
	) throws ArgumentParseException {
		// try one after the other:
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		List<@NonNull FallbackArgumentException> fallbacks;
		if (!parsingFallbacks) {
			fallbacks = new ArrayList<>();
		} else {
			// Not used.
			fallbacks = Collections.emptyList();
		}

		boolean nullParsed = false;
		ArgumentRejectedException rejectedException = null;
		ArgumentParseException firstParseException = null;
		for (@NonNull I input : inputs) {
			try {
				Pair<? extends CommandArgument<?>, @NonNull ?> result = parser.parse(input);
				if (result != null) {
					// We successfully parsed something:
					return result;
				} else {
					nullParsed = true;
					// Continue: Maybe some other argument can parse something more useful..
				}
			} catch (FallbackArgumentException e) {
				if (!parsingFallbacks) {
					// Ignore, but keep track of all fallback exceptions:
					// Assert: fallbacks is modifiable.
					fallbacks.add(e);
				} else {
					Validate.State.error("Argument '" + e.getArgument().getName()
							+ "' threw another FallbackArgumentException while parsing fallback: "
							+ e);
				}
			} catch (ArgumentRejectedException e) {
				// Ignore, but keep track of the first argument-rejected exception:
				if (rejectedException == null) {
					rejectedException = e;
				}
			} catch (ArgumentParseException e) {
				// Ignore, but keep track of the first exception:
				if (firstParseException == null) {
					firstParseException = e;
				}
			}
			// Reset state and continue:
			argsReader.setState(argsReaderState);
		}

		if (!fallbacks.isEmpty()) {
			// If some argument might be able to provide a fallback, prefer following that path:
			// TODO But if the fallbacks fail, we might want to prefer null, rejected or first
			// exception
			// We throw our own custom FallbackArgumentException so that:
			// * We get informed when the fallbacks get evaluated (so that we can store the result)
			// * We can evaluate all the fallbacks we captured (in case there are multiple
			// applicable fallbacks)
			throw new FirstOfFallbackException(this, fallbacks);
		}

		if (nullParsed) {
			// If one argument did return null as parsing result, and did not throw an exception
			// (like optional arguments tend to do), we do not throw an exception here either:
			return null;
		}

		if (rejectedException != null) {
			// Some argument was able to parse something but rejected the result due to some filter.
			// Prefer this more specific exception since it is likely to be more relevant to the
			// user.
			throw rejectedException;
		}

		// Invalid argument for all of them:
		assert firstParseException != null; // Otherwise we would have parsed something (or null)
		throw Unsafe.assertNonNull(firstParseException);
	}

	private @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> toPair(
			CommandArgument<?> argument,
			@Nullable Object value
	) {
		if (value == null) return null;
		@NonNull Object notNullValue = value;
		return Pair.of(argument, notNullValue);
	}

	@Override
	public @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// With modifiable context, so that the child argument can store its value(s):
		Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> result = this.parseFirstOf(
				arguments,
				(argument) -> {
					return this.toPair(argument, argument.parse(input, context, argsReader));
				},
				argsReader,
				false
		);

		// Store result in context:
		if (result != null) {
			context.put(this.getName(), result);
		}
		return result;
	}

	@Override
	public @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// With unmodifiable context view:
		return this.parseFirstOf(arguments, (argument) -> {
			return this.toPair(argument, argument.parseValue(input, context, argsReader));
		}, argsReader, false);
	}

	@Override
	public List<? extends @NonNull String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		List<@NonNull String> suggestions = new ArrayList<>();
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		for (CommandArgument<?> argument : arguments) {
			int limit = (MAX_SUGGESTIONS - suggestions.size());
			if (limit <= 0) break;

			// Reset args so that every argument has a chance to provide completions:
			argsReader.setState(argsReaderState);

			List<? extends @NonNull String> argumentSuggestions = argument.complete(
					input,
					context,
					argsReader
			);
			if (argumentSuggestions.size() < limit) {
				suggestions.addAll(argumentSuggestions);
			} else {
				suggestions.addAll(argumentSuggestions.subList(0, limit));
				break;
			}
		}
		return Collections.unmodifiableList(suggestions);
	}

	private static class FirstOfFallbackException extends FallbackArgumentException {

		private static final long serialVersionUID = -1177782345537954263L;

		private final List<? extends @NonNull FallbackArgumentException> originalFallbacks;

		public FirstOfFallbackException(
				FirstOfArgument firstOfArgument,
				List<? extends @NonNull FallbackArgumentException> originalFallbacks
		) {
			super(firstOfArgument, originalFallbacks.get(0));
			assert originalFallbacks != null && !originalFallbacks.isEmpty();
			this.originalFallbacks = originalFallbacks;
		}

		public List<? extends @NonNull FallbackArgumentException> getOriginalFallbacks() {
			return originalFallbacks;
		}
	}

	@Override
	public @Nullable Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException {
		// Delegate the fallback parsing to the child arguments that threw the original
		// FallbackArgumentExceptions:
		FirstOfFallbackException firstOfFallback = (FirstOfFallbackException) fallbackException;
		List<? extends @NonNull FallbackArgumentException> originalFallbacks = firstOfFallback.getOriginalFallbacks();

		Pair<? extends @NonNull CommandArgument<?>, @NonNull ?> result = this.parseFirstOf(
				originalFallbacks,
				(fallback) -> {
					FallbackArgument<?> argument = fallback.getArgument();
					Object value = argument.parseFallback(
							input,
							context,
							argsReader,
							fallback,
							parsingFailed
					);
					return this.toPair(argument, value);
				},
				argsReader,
				true
		);

		// Store result in context:
		if (result != null) {
			context.put(this.getName(), result);
		}
		return result;
	}
}
