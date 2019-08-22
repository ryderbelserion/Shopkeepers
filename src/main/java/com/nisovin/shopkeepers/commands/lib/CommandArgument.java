package com.nisovin.shopkeepers.commands.lib;

import java.util.List;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class CommandArgument {

	public static final String REQUIRED_FORMAT_PREFIX = "<";
	public static final String REQUIRED_FORMAT_SUFFIX = ">";

	public static final String OPTIONAL_FORMAT_PREFIX = "[";
	public static final String OPTIONAL_FORMAT_SUFFIX = "]";

	private final String name;

	public CommandArgument(String name) {
		Validate.notEmpty(StringUtils.removeWhitespace(name), "Invalid argument name!");
		this.name = name;
	}

	/**
	 * Gets the argument name.
	 * 
	 * @return the argument name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Checks whether this {@link CommandArgument} is optional.
	 * <p>
	 * Optional arguments have slightly different behavior:
	 * <ul>
	 * <li>If no value can be parsed, no {@link ArgumentParseException} is thrown, but <code>null</code> is returned
	 * instead.
	 * <li>A different format is used to visually mark them as optional.
	 * </ul>
	 * <p>
	 * For making a given {@link CommandArgument} optional, {@link OptionalArgument} may be used.
	 * 
	 * @return <code>true</code> if this argument is optional, <code>false</code> otherwise
	 */
	public boolean isOptional() {
		return false;
	}

	// TODO somehow allow the translation of argument names (for displaying purposes inside the command help)?
	/**
	 * Gets the usage format for this argument.
	 * <p>
	 * This may be used for displaying purposes.<br>
	 * Note for argument implementations overriding this method: It is recommended to make use of
	 * {@link #getReducedFormat()} when generating the format, because certain types of arguments which consist of
	 * several child-arguments might only modify the reduced format and expect those changes to get carried over into
	 * the argument's format.
	 * 
	 * @return the argument format
	 */
	public String getFormat() {
		if (this.isOptional()) {
			return OPTIONAL_FORMAT_PREFIX + this.getReducedFormat() + OPTIONAL_FORMAT_SUFFIX;
		} else {
			return REQUIRED_FORMAT_PREFIX + this.getReducedFormat() + REQUIRED_FORMAT_SUFFIX;
		}
	}

	/**
	 * {@link CommandArgument}s can implement this to return a reduced format for this argument.
	 * <p>
	 * Usually the reduced format does simply not contain any surrounding brackets and gets then used by
	 * {@link #getFormat()} or by other {@link CommandArgument}s which wrap this reduced format into their format.
	 * 
	 * @return the reduced format
	 */
	public String getReducedFormat() {
		return name;
	}

	/**
	 * Gets the 'missing argument' error message.
	 * 
	 * @return the error message
	 */
	public String getMissingArgumentErrorMsg() {
		return TextUtils.replaceArgs(Settings.msgCommandArgumentMissing,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat());
	}

	/**
	 * Gets the 'invalid argument' error message.
	 * 
	 * @param argument
	 *            the argument input
	 * @return the error message
	 */
	public String getInvalidArgumentErrorMsg(String argument) {
		if (argument == null) argument = "";
		return TextUtils.replaceArgs(Settings.msgCommandArgumentInvalid,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat(),
				"{argument}", argument);
	}

	/**
	 * This creates an {@link ArgumentParseException} with the 'missing argument' error message.
	 * 
	 * @return the created exception
	 * @see #getMissingArgumentErrorMsg()
	 */
	protected final ArgumentParseException missingArgument() {
		return new ArgumentParseException(this.getMissingArgumentErrorMsg());
	}

	/**
	 * This creates an {@link ArgumentParseException} with the 'invalid argument' error message.
	 * 
	 * @return the created exception
	 * @see #getInvalidArgumentErrorMsg(String)
	 * 
	 * @param argument
	 *            the invalid argument input from the {@link CommandArgs}
	 */
	protected final ArgumentParseException invalidArgument(String argument) {
		return new ArgumentParseException(this.getInvalidArgumentErrorMsg(argument));
	}

	/**
	 * Parses a value for this argument via {@link #parseValue(CommandInput, CommandArgs)} and stores it in the
	 * {@link CommandContext} with the argument's name as key.
	 * <p>
	 * This moves the cursor of the {@link CommandArgs} forward for all successfully used-up arguments. In case parsing
	 * fails, the state of the {@link CommandArgs} gets restored.<br>
	 * If parsing returns <code>null</code>, then nothing gets stored in the {@link CommandContext}.<br>
	 * Optional arguments will not throw any exceptions caused by parsing, but will instead handle it as if parsing
	 * returned <code>null</code>. See {@link #isOptional()}.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the context which will store the parsed value
	 * @param args
	 *            the command arguments from which the value will get extracted from
	 * @throws ArgumentParseException
	 *             if unable to parse a value and not optional
	 */
	public void parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		Object value;
		try {
			value = this.parseValue(input, args);
		} catch (ArgumentParseException e) {
			// restoring previous args state:
			args.setState(state);

			if (this.isOptional()) {
				// set value to null:
				value = null;
			} else {
				// pass on exception:
				throw e;
			}
		}
		if (value != null) {
			context.put(name, value);
		}
	}

	/**
	 * Parses a value for this argument from the given {@link CommandArgs} and moves its cursor forward for every
	 * successfully used-up argument.
	 * 
	 * @param input
	 *            the input
	 * @param args
	 *            the command arguments
	 * @return the parsed value, or <code>null</code> to indicate that nothing was parsed
	 * @throws ArgumentParseException
	 *             if unable to parse a value
	 */
	public abstract Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException;

	/**
	 * This provides suggestions for the last argument, IF parsing this argument would use up the last argument of
	 * the {@link CommandArgs}.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the command context
	 * @param args
	 *            the command arguments, including the final, possibly partial or empty argument
	 * @return the suggestions for the final argument, <code>null</code> or empty to indicate 'no suggestions'
	 */
	public abstract List<String> complete(CommandInput input, CommandContext context, CommandArgs args);
}
