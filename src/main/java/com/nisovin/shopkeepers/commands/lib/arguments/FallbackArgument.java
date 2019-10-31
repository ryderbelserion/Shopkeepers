package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
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
 * this ambiguity, the fallback command argument doesn't get evaluated immediately, but only after checking if the
 * following command arguments are able to parse the remaining input arguments. This gets indicated to the parsing
 * command by throwing a {@link FallbackArgumentException}.
 * <p>
 * Before evaluating the fallback, the context gets reset to the original state from before the fallback. If the parsing
 * of the following command arguments succeeds, the parsing of the fallback argument gets invoked with no remaining
 * input arguments. If the parsing of the following command arguments fails, the arguments reader gets reset to the
 * original state before evaluating the fallback argument.</br>
 * If the fallback succeeds and consumes arguments, the parsing restarts from there with the next command argument. If
 * the fallback succeeds but no arguments were consumed, any context changes from parsing the following command
 * arguments get applied and then the parsing either ends (either successfully, or with the parsing error from before
 * the fallback evaluation) or any other pending fallback gets evaluated.</br>
 * If the fallback fails, the fallback's parsing error gets used and then parsing either ends with that error or any
 * other pending fallback gets evaluated.</br>
 * If there are remaining unparsed arguments after the fallback got evaluated (regardless of whether it failed or
 * succeeded), the original parsing error of this command argument is used and parsing either fails with that error or
 * any other pending fallback gets evaluated.
 * <p>
 * {@link FallbackArgument}s can be chained. When the fallback gets evaluated it checks for and evaluates chained child
 * fallbacks first. Just like regular {@link CommandArgument}s the fallback throws an {@link ArgumentParseException} if
 * it is not optional and cannot provide a fallback value. But throwing another {@link FallbackArgumentException} is not
 * allowed at this point and will lead to an error. The actual fallback arguments are therefore not allowed to be
 * {@link FallbackArgument}s themselves.
 */
public class FallbackArgument<T> extends CommandArgument<T> {

	protected final CommandArgument<T> argument; // may be a fallback argument itself
	protected final CommandArgument<T> fallbackArgument;

	public FallbackArgument(CommandArgument<T> argument, CommandArgument<T> fallbackArgument) {
		super(Validate.notNull(argument).getName());
		Validate.notNull(fallbackArgument, "Fallback argument is null!");
		Validate.isTrue(!(fallbackArgument instanceof FallbackArgument), "Fallback argument cannot be a FallbackArgument itself!");
		this.argument = argument;
		this.fallbackArgument = fallbackArgument;

		argument.setParent(this);
		fallbackArgument.setParent(this);
	}

	// may itself by a FallbackArgument
	public CommandArgument<T> getOriginalArgument() {
		return argument;
	}

	public CommandArgument<T> getFallbackArgument() {
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
	public String getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argumentInput) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@Override
	public T parse(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		try {
			return argument.parse(input, context, argsReader);
		} catch (ArgumentParseException e) {
			// Note: Caller is responsible for resetting the args reader if required
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		try {
			return argument.parseValue(input, context, argsReader);
		} catch (ArgumentParseException e) {
			// Note: Caller is responsible for resetting the args reader if required
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	// parsingFailed: Whether parsing the following command arguments failed. The arguments reader got reset to the
	// original state in that case. Otherwise (if parsing succeeded) the arguments reader will have no remaining
	// unparsed arguments.
	public T parseFallback(	CommandInput input, CommandContext context, ArgumentsReader argsReader,
							FallbackArgumentException fallbackException, boolean parsingFailed) throws ArgumentParseException {
		// Fallback chaining: If the original exception was a fallback itself, try it first
		Optional<T> originalFallbackValue = this.parseOriginalFallback(input, context, argsReader, fallbackException, parsingFailed);
		if (originalFallbackValue != null) {
			return originalFallbackValue.orElse(null);
		}

		// try to parse fallback argument:
		try {
			return fallbackArgument.parse(input, context, argsReader);
		} catch (MissingArgumentException | RequiresPlayerArgumentException e) {
			// If the fallback throws a 'missing argument' or 'requires player' exception, prefer the parsing error of
			// the original argument (TODO only if the original error has been / is (after re-evaluating it) a 'missing
			// argument' or 'requires player' error as well?):
			if (parsingFailed) {
				// The arguments reader got reset, so re-evaluating the original command arguments is not expected to be
				// required / to yield a different outcome:
				throw fallbackException.getRootException();
			} else {
				// Parsing past the fallback argument succeeded, so the original argument was likely in a different
				// situation before than it is the case now. Re-evaluate the original argument will therefore likely
				// yield a different error now (likely 'missing argument' or 'requires players').
				// We can't however be sure that this is the case since we don't know the nature / internals of the
				// original argument. It might for example be an argument that doesn't consume any arguments and
				// therefore we can't simple throw a 'missing argument'/'requires player' exception in its name here.
				try {
					return argument.parse(input, context, argsReader);
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
	protected final Optional<T> parseOriginalFallback(	CommandInput input, CommandContext context, ArgumentsReader argsReader,
														FallbackArgumentException fallbackException, boolean parsingFailed) {
		ArgumentParseException originalException = fallbackException.getOriginalException();
		if (originalException instanceof FallbackArgumentException) {
			FallbackArgumentException originalFallback = (FallbackArgumentException) originalException;
			FallbackArgument<?> originalFallbackArgument = originalFallback.getFallbackArgument();
			try {
				// if the original fallback succeeds, skip our fallback:
				// the result may be empty (null)
				// the original fallback is expected to be of the same type
				return Optional.ofNullable((T) originalFallbackArgument.parseFallback(input, context, argsReader, originalFallback, parsingFailed));
			} catch (FallbackArgumentException e) { // fallback is not allowed to throw another fallback exception here
				Validate.State.error("Original fallback argument '" + originalFallbackArgument.getName()
						+ "' threw another FallbackArgumentException while parsing fallback: " + e);
			} catch (ArgumentParseException e) {
				// the original fallback failed: ignore and return null
			}
		}
		return null;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		// combine suggestions of the original and the fallback argument:
		ArgumentsReader argsReaderState = argsReader.createSnapshot(); // keep track of the initial state
		List<String> argumentSuggestions = argument.complete(input, context, argsReader);
		if (argumentSuggestions.size() >= MAX_SUGGESTIONS) return argumentSuggestions;

		List<String> suggestions = new ArrayList<>(argumentSuggestions);
		int limit = (MAX_SUGGESTIONS - suggestions.size());
		assert limit > 0;

		// reset args so that the fallback argument has a chance to provide different completions:
		argsReader.setState(argsReaderState);
		List<String> fallbackSuggestions = fallbackArgument.complete(input, context, argsReader);

		if (fallbackSuggestions.size() <= limit) {
			suggestions.addAll(fallbackSuggestions);
		} else {
			suggestions.addAll(fallbackSuggestions.subList(0, limit));
		}
		return Collections.unmodifiableList(suggestions);
	}
}
