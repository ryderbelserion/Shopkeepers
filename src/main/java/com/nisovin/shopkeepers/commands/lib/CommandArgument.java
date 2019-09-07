package com.nisovin.shopkeepers.commands.lib;

import java.util.List;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * @param <T>
 *            the type of the parsed argument
 */
public abstract class CommandArgument<T> {

	/**
	 * The recommended default limit on the number of command suggestions.
	 */
	public static final int MAX_SUGGESTIONS = 20;

	public static final String REQUIRED_FORMAT_PREFIX = "<";
	public static final String REQUIRED_FORMAT_SUFFIX = ">";

	public static final String OPTIONAL_FORMAT_PREFIX = "[";
	public static final String OPTIONAL_FORMAT_SUFFIX = "]";

	private final String name;

	/**
	 * Create a new {@link CommandArgument}.
	 * <p>
	 * The argument name can not contain any whitespace and should be unique among all arguments of the same command.
	 * <p>
	 * Some compound arguments may delegate parsing to internal arguments. To avoid naming conflicts, the character
	 * <code>:</code> is reserved for use by those internal arguments.
	 * 
	 * @param name
	 *            the argument's name
	 */
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
	 * Optional command arguments do not strictly require any input, but may for example provide a fallback or default
	 * value if no value is specified by the user.
	 * <p>
	 * The return value of this method alone does not change the argument's parsing behavior, but only acts as indicator
	 * for other components. This is for example used to pick a different format to visually mark this argument as
	 * optional.
	 * <p>
	 * For making a given {@link CommandArgument} optional, {@link OptionalArgument} can be used.
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
	 * <p>
	 * The returned format can be empty for 'hidden arguments'. Often these don't require any textual user input, but
	 * may still inject information into the {@link CommandContext} depending on the context of command execution. An
	 * example would be an argument that requires the user to target a specific block or entity when executing the
	 * command. These hidden arguments may also be useful as components of other (non-hidden) arguments, for example to
	 * inject defaults for optional arguments. Another use could be to actually hide optional textual arguments, for
	 * example optional debugging options.
	 * 
	 * @return the argument format, not <code>null</code>, but may be empty for hidden arguments
	 */
	public String getFormat() {
		String reducedFormat = this.getReducedFormat();
		if (reducedFormat.isEmpty()) {
			return "";
		} else if (this.isOptional()) {
			return OPTIONAL_FORMAT_PREFIX + reducedFormat + OPTIONAL_FORMAT_SUFFIX;
		} else {
			return REQUIRED_FORMAT_PREFIX + reducedFormat + REQUIRED_FORMAT_SUFFIX;
		}
	}

	/**
	 * {@link CommandArgument}s can implement this to return a reduced format for this argument.
	 * <p>
	 * Usually the reduced format does simply not contain any surrounding brackets and gets then used by
	 * {@link #getFormat()} or by other {@link CommandArgument}s which wrap this reduced format into their format.
	 * <p>
	 * The returned format can be empty for 'hidden arguments'. Often these don't require any textual user input, but
	 * may still inject information into the {@link CommandContext} depending on the context of command execution. An
	 * example would be an argument that requires the user to target a specific block or entity when executing the
	 * command. These hidden arguments may also be useful as components of other (non-hidden) arguments, for example to
	 * inject defaults for optional arguments. Another use could be to actually hide optional textual arguments, for
	 * example optional debugging options.
	 * 
	 * @return the reduced format, not <code>null</code>, but may be empty for hidden arguments
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
	 * fails, the state of the {@link CommandArgs} gets restored.
	 * <p>
	 * If parsing returns <code>null</code> (i.e. for optional arguments), then nothing gets stored in the
	 * {@link CommandContext}.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the context which will store the parsed value
	 * @param args
	 *            the command arguments from which the value will get extracted from
	 * @throws ArgumentParseException
	 *             if unable to parse a value
	 */
	public void parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		T value;
		try {
			value = this.parseValue(input, args);
		} catch (ArgumentParseException e) {
			// restore previous args state:
			args.setState(state);
			throw e;
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
	public abstract T parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException;

	/**
	 * This provides suggestions for the last argument, IF parsing this argument would use up the last argument of
	 * the {@link CommandArgs}.
	 * <p>
	 * Don't expect the returned suggestions list to be mutable. However, <code>null</code> is not a valid return value,
	 * neither for the suggestions list nor its contents.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the command context
	 * @param args
	 *            the command arguments, including the final, possibly partial or empty argument
	 * @return the suggestions for the final argument, or an empty list to indicate 'no suggestions' (not
	 *         <code>null</code> and not containing <code>null</code>)
	 */
	public abstract List<String> complete(CommandInput input, CommandContext context, CommandArgs args);
}
