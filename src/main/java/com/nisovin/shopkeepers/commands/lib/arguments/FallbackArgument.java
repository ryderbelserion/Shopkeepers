package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.MissingArgumentException;
import com.nisovin.shopkeepers.commands.lib.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps two other {@link CommandArgument command arguments}: If parsing the first command argument fails, the second
 * command argument gets evaluated as fallback.
 * <p>
 * Parsing of the first wrapped command argument may fail because the current argument is invalid, or because the
 * command argument is optional and the current argument is meant to bind to the next command argument. To deal with
 * this ambiguity, the fallback command argument doesn't get evaluated immediately, but only after giving the following
 * command arguments a chance to parse the current argument. This gets indicated to the parsing command by throwing a
 * {@link FallbackArgumentException}.
 * <p>
 * Once the fallback command argument gets evaluated, it may or may not consume arguments. It may also throw an
 * {@link ArgumentParseException} itself, if no fallback value can be determined. Any {@link FallbackArgumentException}
 * thrown at this point will simply get evaluated immediately by the processing command (it is left to the command
 * argument implementation to ensure that no infinite loops are caused by this recursion).
 */
public class FallbackArgument<T> extends CommandArgument<T> {

	protected final CommandArgument<T> argument;
	protected final CommandArgument<T> fallbackArgument;

	public FallbackArgument(CommandArgument<T> argument, CommandArgument<T> fallbackArgument) {
		super(Validate.notNull(argument).getName());
		Validate.notNull(fallbackArgument);
		this.argument = argument;
		this.fallbackArgument = fallbackArgument;

		argument.setParent(this);
		fallbackArgument.setParent(this);
	}

	public CommandArgument<T> getOriginalArgument() {
		if (argument instanceof FallbackArgument) {
			return ((FallbackArgument<T>) argument).getOriginalArgument();
		} else {
			return argument;
		}
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
	public String getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argumentInput) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@Override
	public T parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		try {
			// TODO also use fallback if argument parses 'null' (eg. for optional arguments)?
			// maybe then prefer null in case the fallback fails?
			return argument.parse(input, context, args);
		} catch (ArgumentParseException e) {
			args.setState(state); // reset arguments
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	@Override
	public T parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		try {
			return argument.parseValue(input, args);
		} catch (ArgumentParseException e) {
			args.setState(state); // reset arguments
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	// parsingContextChanged: whether some aspect of the parsing context (input, context, args) might have changed
	// compared to when the original argument tried to parse
	// the CommandArgs may be empty if some other command argument was able to successfully parse it in the meantime
	// Assumption: The args get reset if parsing fails
	public T parseFallback(	CommandInput input, CommandContext context, CommandArgs args, boolean parsingContextChanged,
							FallbackArgumentException fallbackException) throws ArgumentParseException {
		// Fallback chaining: If the original exception was a fallback itself, try it first
		Optional<T> originalFallbackValue = this.parseOriginalFallback(input, context, args, parsingContextChanged, fallbackException);
		if (originalFallbackValue != null) {
			return originalFallbackValue.orElse(null);
		}

		// try to parse fallback argument:
		try {
			return fallbackArgument.parse(input, context, args);
		} catch (MissingArgumentException | RequiresPlayerArgumentException e) {
			// If the fallback throws a 'missing argument' or 'requires player' exception, prefer the parsing error of
			// the original argument:
			if (!parsingContextChanged) {
				// Reevaluating is not expected to be required if the parsing context is still the same:
				throw fallbackException.getRootException();
			} else {
				// Reevaluate the original argument:
				// Usually this is equivalent to rethrowing the original root exception, EXCEPT if some other argument
				// was able to parse the presumable invalid argument that lead to the original parsing error. In this
				// case the root argument will now throw a different (likely 'missing argument') exception. We can't
				// however be sure that this is the case since we don't know the nature / internals of the original
				// argument. It might for example be an argument that doesn't consume any args and therefore we can't
				// simple throw a 'missing argument' exception in its name here.
				try {
					return argument.parse(input, context, args);
					// unexpected, but in case the original argument succeeds now, use the parsed value
				} catch (FallbackArgumentException fe) {
					// in case we are inside a chain of fallbacks, use the original (root) exception:
					throw fe.getRootException();
				} // forward any other type of parsing exception
			}
		}
	}

	// returns null if no original fallback was able to parse a fallback value
	@SuppressWarnings("unchecked")
	protected final Optional<T> parseOriginalFallback(	CommandInput input, CommandContext context, CommandArgs args,
														boolean parsingContextChanged, FallbackArgumentException fallbackException) {
		ArgumentParseException originalException = fallbackException.getOriginalException();
		if (originalException instanceof FallbackArgumentException) {
			FallbackArgumentException originalFallback = (FallbackArgumentException) originalException;
			while (true) {
				FallbackArgument<?> fallbackArgument = originalFallback.getFallbackArgument();
				try {
					// if the original fallback succeeds, skip our fallback:
					// the result may be empty (null)
					// the original fallback is expected to be of the same type
					return Optional.ofNullable((T) fallbackArgument.parseFallback(input, context, args, parsingContextChanged, originalFallback));
				} catch (FallbackArgumentException e) {
					// got another fallback: evaluate it immediately in the next loop iteration
					originalFallback = e;
					continue;
				} catch (ArgumentParseException e) {
					// the original fallback failed:
					break;
				}
			}
		}
		return null;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		// combine suggestions of the original and the fallback argument:
		Object state = args.getState(); // keep track of the initial state
		List<String> argumentSuggestions = argument.complete(input, context, args);
		if (argumentSuggestions.size() >= MAX_SUGGESTIONS) return argumentSuggestions;

		List<String> suggestions = new ArrayList<>(argumentSuggestions);
		int limit = (MAX_SUGGESTIONS - suggestions.size());
		assert limit > 0;

		// reset args so that the fallback argument has a chance to provide different completions:
		args.setState(state);
		List<String> fallbackSuggestions = fallbackArgument.complete(input, context, args);

		if (fallbackSuggestions.size() <= limit) {
			suggestions.addAll(fallbackSuggestions);
		} else {
			suggestions.addAll(fallbackSuggestions.subList(0, limit));
		}
		return Collections.unmodifiableList(suggestions);
	}
}
