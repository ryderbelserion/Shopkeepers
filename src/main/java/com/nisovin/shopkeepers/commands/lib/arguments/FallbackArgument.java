package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps another {@link CommandArgument} and provides a fallback if parsing fails.
 * <p>
 * Parsing of the wrapped command argument may fail because the current argument is invalid, or because the wrapped
 * command argument is optional and the current argument is meant to bind to the next command argument. To deal with
 * this ambiguity, the fallback doesn't get evaluated immediately, but only after giving the following command arguments
 * a chance to parse the current argument. This gets indicated to the parsing command by throwing a
 * {@link FallbackArgumentException}.
 * <p>
 * Once the fallback gets evaluated, it may or may not consume arguments. It may also throw an
 * {@link ArgumentParseException} itself, if no fallback value can be determined. Any {@link FallbackArgumentException}
 * thrown at this point will simply get evaluated immediately by the processing command (it is left to the argument
 * implementation to ensure that no infinite loops are caused by this recursion).
 */
public abstract class FallbackArgument<T> extends CommandArgument<T> {

	protected final CommandArgument<T> argument;

	public FallbackArgument(CommandArgument<T> argument) {
		super(argument.getName());
		Validate.notNull(argument);
		this.argument = argument;
		argument.setParent(this);
	}

	@Override
	public String getReducedFormat() {
		return argument.getReducedFormat();
	}

	/**
	 * Whether this {@link FallbackArgument} has a fallback that doesn't consume any arguments (even if that may fail).
	 * <p>
	 * This gets used to determine whether this argument should be marked as 'optional' or not.
	 * 
	 * @return <code>true</code> if there is a possible fallback that doesn't require any input arguments from the user
	 */
	public abstract boolean hasNoArgFallback();

	@Override
	public boolean isOptional() {
		return this.hasNoArgFallback() || argument.isOptional();
	}

	@Override
	public String getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argumentInput) {
		return this.argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	@Override
	public void parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		try {
			argument.parse(input, context, args);
		} catch (ArgumentParseException e) {
			args.setState(state); // reset arguments
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	@Override
	public T parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		try {
			// let the wrapped argument try to parse:
			return argument.parseValue(input, args);
		} catch (ArgumentParseException e) {
			args.setState(state); // reset arguments
			throw new FallbackArgumentException(this, e); // throw fallback exception
		}
	}

	// the CommandArgs may be empty if some other command argument was able to successfully parse it in the meantime
	public void parseFallback(CommandInput input, CommandContext context, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		// Fallback chaining: If the original exception was a fallback itself, try it first
		ArgumentParseException originalException = fallbackException.getOriginalException();
		if (originalException instanceof FallbackArgumentException) {
			FallbackArgumentException originalFallback = (FallbackArgumentException) originalException;
			while (true) {
				FallbackArgument<?> fallbackArgument = originalFallback.getArgument();
				try {
					fallbackArgument.parseFallback(input, context, args, originalFallback);
				} catch (FallbackArgumentException e) {
					// got another fallback: evaluate it immediately in the next loop iteration
					originalFallback = e;
					continue;
				} catch (ArgumentParseException e) {
					// the original fallback failed, continue with our fallback
					break;
				}
				// the original fallback succeeded, skip our fallback:
				return;
			}
		}

		// just like regular parse, but uses the fallback value:
		Object state = args.getState();
		T value;
		try {
			value = this.parseFallbackValue(input, args, fallbackException);
		} catch (ArgumentParseException e) {
			// restore previous args state:
			args.setState(state);
			throw e;
		}
		if (value != null) {
			context.put(this.getName(), value);
		}
	}

	public abstract T parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException;

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return argument.complete(input, context, args);
	}
}
