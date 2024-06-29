package com.nisovin.shopkeepers.commands.lib.argument.fallback;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;

/**
 * A {@link CommandArgument} that can provide a fallback if parsing fails.
 * <p>
 * Parsing may fail because the current input argument is either invalid, or because the command
 * argument is optional and the current input argument is meant to bind to the next command argument
 * instead. To deal with this ambiguity, the fallback doesn't get evaluated immediately, but only
 * after checking if the following command arguments are able to parse the remaining input
 * arguments. This gets indicated to the parsing command by throwing a
 * {@link FallbackArgumentException}.
 * <p>
 * Before evaluating the fallback, the context gets reset to the original state from before the
 * parsing of this command argument. If the parsing of the following command arguments succeeds, the
 * parsing of the fallback gets invoked with no remaining input arguments. If the parsing of the
 * following command arguments fails, the {@link ArgumentsReader} gets reset to the original state
 * before evaluating the fallback.<br>
 * If the fallback succeeds and consumes arguments, the parsing restarts from there with the next
 * command argument. If the fallback succeeds but no arguments were consumed, any context changes
 * from parsing the following command arguments get applied and then the parsing either ends (either
 * successfully, or with the parsing error from before the fallback evaluation) or any other pending
 * fallback gets evaluated.<br>
 * If the fallback fails, the fallback's parsing error gets used and then parsing either ends with
 * that error or any other pending fallback gets evaluated.<br>
 * If there are remaining unparsed arguments after the fallback got evaluated (regardless of whether
 * it failed or succeeded), the original parsing error of this command argument is used and parsing
 * either fails with that error or any other pending fallback gets evaluated.
 * <p>
 * Just like regular {@link CommandArgument}s the fallback throws an {@link ArgumentParseException}
 * if it is not optional and cannot provide a fallback value. But throwing another
 * {@link FallbackArgumentException} is not allowed at this point and will lead to an error.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public abstract class FallbackArgument<T> extends CommandArgument<T> {
	// TODO Integrate into CommandArgument? Some arguments with child arguments are only supposed to
	// 'be' fallback arguments if their child arguments are fallback arguments. This leads to them
	// having to always extend FallbackArgument anyway, regardless of whether they actually use
	// fallbacks or not.
	// TODO Avoid wrapping FallbackArgumentsExceptions of child arguments? Delegate fallback parsing
	// to parent/root argument inside Command?

	public FallbackArgument(String name) {
		super(name);
	}

	/**
	 * Parses a fallback value for this argument and stores it in the {@link CommandContext} with
	 * the argument's name as key.
	 * <p>
	 * This gets invoked after regular parsing threw a {@link FallbackArgumentException} and parsing
	 * past this command argument of the remaining input arguments either failed or succeeded. If
	 * parsing past this command argument failed, the {@link ArgumentsReader} got reset to the
	 * original state. Otherwise, if parsing succeeded, the {@link ArgumentsReader} will have no
	 * remaining unparsed arguments.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the context which stores the parsed argument values, not <code>null</code>
	 * @param argsReader
	 *            the ArgumentsReader, not <code>null</code>
	 * @param fallbackException
	 *            the fallback exception that indicated this fallback, not <code>null</code>
	 * @param parsingFailed
	 *            whether parsing past this command argument failed
	 * @return the parsed value, or <code>null</code> if nothing was parsed (e.g. for optional
	 *         fallbacks)
	 * @throws ArgumentParseException
	 *             if unable to parse a value for a non-optional fallback, but not allowed to throw
	 *             another {@link FallbackArgumentException}
	 * @see CommandArgument#parse(CommandInput, CommandContext, ArgumentsReader)
	 */
	public abstract T parseFallback(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader,
			FallbackArgumentException fallbackException,
			boolean parsingFailed
	) throws ArgumentParseException;
}
