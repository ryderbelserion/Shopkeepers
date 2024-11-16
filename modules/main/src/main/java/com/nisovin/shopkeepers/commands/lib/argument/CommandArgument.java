package com.nisovin.shopkeepers.commands.lib.argument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * A command component responsible for parsing and providing completion suggestions for a portion of
 * the command's input arguments.
 * 
 * @param <T>
 *            the type of the parsed argument, can be nullable for optional arguments
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
	private @Nullable String displayName = null; // Null to use default (name), not empty
	// Null if not yet set, empty if it has no parent:
	@SuppressWarnings("annotations.on.use")
	private @Nullable Optional<CommandArgument<?>> parent = null;

	/**
	 * Create a new {@link CommandArgument}.
	 * <p>
	 * The argument name can not contain any whitespace and has to be unique among all arguments of
	 * the same command.
	 * <p>
	 * Some command arguments may delegate parsing to internal arguments. To avoid naming conflicts,
	 * the character <code>:</code> is reserved for use by those internal arguments.
	 * 
	 * @param name
	 *            the argument's name, not <code>null</code> or empty
	 */
	public CommandArgument(String name) {
		Validate.notEmpty(name, "name is null or empty");
		Validate.isTrue(!StringUtils.containsWhitespace(name), "name contains whitespace");
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
	 * Gets the argument's display name.
	 * <p>
	 * This is used when representing this argument inside the argument format. By default, this
	 * matches the argument's name.
	 * 
	 * @return the argument's display name
	 */
	public final String getDisplayName() {
		return (displayName != null) ? displayName : name;
	}

	/**
	 * Sets the argument's display name.
	 * 
	 * @param displayName
	 *            the new display name, or <code>null</code> to use the default
	 * @return this
	 */
	public CommandArgument<T> setDisplayName(@Nullable String displayName) {
		if (displayName != null) {
			Validate.notEmpty(displayName, "displayName is empty");
			Validate.isTrue(!StringUtils.containsWhitespace(displayName),
					"displayName contains whitespace");
		}
		// Normalize default display name to null:
		if (this.getName().equals(displayName)) {
			this.displayName = null;
		} else {
			this.displayName = displayName;
		}
		return this;
	}

	/**
	 * Sets the parent argument.
	 * <p>
	 * The parent can only be set once, and it cannot be set after this argument has already been
	 * added to a {@link Command}.
	 * 
	 * @param parent
	 *            the parent argument, or <code>null</code> to indicate that this argument has no
	 *            parent
	 */
	public final void setParent(@Nullable CommandArgument<?> parent) {
		Validate.State.isTrue(this.parent == null, "Parent has already been set!");
		Validate.isTrue(parent != this, "Cannot set parent to self!");
		this.parent = Optional.ofNullable(parent); // Can be empty
	}

	/**
	 * Gets the parent argument.
	 * <p>
	 * The parent being <code>null</code> indicates that either the parent hasn't been set yet, or
	 * this argument is a top-level (root) argument. Otherwise, this argument is used internally by
	 * another argument (the parent).
	 * 
	 * @return the parent argument, can be <code>null</code>
	 */
	public final @Nullable CommandArgument<?> getParent() {
		return (parent != null) ? parent.orElse(null) : null;
	}

	/**
	 * Gets the root argument.
	 * <p>
	 * This follows the chain of parent arguments to the top-level argument which itself does not
	 * have any parent. If this argument does not have a parent, then this argument itself is
	 * returned.
	 * 
	 * @return the root argument, not <code>null</code>
	 */
	public final CommandArgument<?> getRootArgument() {
		CommandArgument<?> current = this;
		CommandArgument<?> currentParent = this.getParent();
		while (currentParent != null) {
			current = currentParent;
			currentParent = current.getParent();
		}
		return current;
	}

	/**
	 * Returns whether this {@link CommandArgument} is optional.
	 * <p>
	 * Optional command arguments do not necessarily require any user input. This includes not only
	 * arguments that are actually optional in the sense that the user can decide to use or not use
	 * some command option, but also arguments that provide some kind of fallback handling that
	 * injects a default or context dependent value if none is specified by the user. The fallback
	 * handling may however fail as well.
	 * <p>
	 * The return value of this method alone does not change the argument's parsing behavior, but
	 * only acts as indicator for other components. This is for example used to pick a different
	 * format to visually mark this argument as optional.
	 * <p>
	 * For making a given {@link CommandArgument} optional, {@link OptionalArgument} can be used.
	 * 
	 * @return <code>true</code> if this argument is optional, <code>false</code> otherwise
	 */
	public boolean isOptional() {
		return false;
	}

	// TODO Somehow allow the translation of argument names (for displaying purposes inside the
	// command help)?
	/**
	 * Gets the usage format for this argument.
	 * <p>
	 * This may be used for displaying purposes.<br>
	 * Note for argument implementations overriding this method: It is recommended to make use of
	 * {@link #getReducedFormat()} when generating the format, because certain types of arguments
	 * which consist of several child-arguments might only modify the reduced format and expect
	 * those changes to get carried over into the argument's format.
	 * <p>
	 * The returned format can be empty for 'hidden arguments'. Often these don't require any
	 * textual user input, but may still inject information into the {@link CommandContext}
	 * depending on the context of command execution. An example would be an argument that requires
	 * the user to target a specific block or entity when executing the command. These hidden
	 * arguments may also be useful as components of other (non-hidden) arguments, for example to
	 * inject defaults for optional arguments. Another use could be to actually hide optional
	 * textual arguments, for example optional debugging options.
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
	 * Usually the reduced format does simply not contain any surrounding brackets and gets then
	 * used by {@link #getFormat()} or by other {@link CommandArgument}s which embed this reduced
	 * format into their format.
	 * <p>
	 * The returned format can be empty for 'hidden arguments'. Often these don't require any
	 * textual user input, but may still inject information into the {@link CommandContext}
	 * depending on the context of command execution. An example would be an argument that requires
	 * the user to target a specific block or entity when executing the command. These hidden
	 * arguments may also be useful as components of other (non-hidden) arguments, for example to
	 * inject defaults for optional arguments. Another use could be to actually hide optional
	 * textual arguments, for example optional debugging options.
	 * 
	 * @return the reduced format, not <code>null</code>, but may be empty for hidden arguments
	 */
	public String getReducedFormat() {
		return this.getDisplayName();
	}

	// COMMON ERRORS

	private final MessageArguments defaultErrorMsgArgs = this.setupDefaultErrorMsgArgs();

	private MessageArguments setupDefaultErrorMsgArgs(CommandArgument<T> this) {
		// Dynamically resolve arguments:
		Map<String, Object> args = new HashMap<>();
		Supplier<?> argumentNameSupplier = () -> {
			CommandArgument<?> rootArgument = Unsafe.initialized(this).getRootArgument();
			return rootArgument.getDisplayName();
		};
		Supplier<?> argumentFormatSupplier = () -> {
			CommandArgument<?> rootArgument = Unsafe.initialized(this).getRootArgument();
			String format = rootArgument.getFormat();
			if (format.isEmpty()) {
				// Use the name in case the format is empty (e.g. for hidden arguments):
				return argumentNameSupplier.get();
			} else {
				return format;
			}
		};
		args.put("argumentName", argumentNameSupplier);
		args.put("argumentFormat", argumentFormatSupplier);
		return MessageArguments.ofMap(args);
	}

	/**
	 * Gets the common default error message arguments.
	 * <p>
	 * This includes:
	 * <ul>
	 * <li><code>argumentName</code> (uses the argument's display name)
	 * <li><code>argumentFormat</code>
	 * </ul>
	 * This uses the {@link #getRootArgument() root argument} for determining the argument name and
	 * format. If the format is empty (e.g. for hidden arguments), the name is used instead.
	 * 
	 * @return the common default error message arguments
	 */
	public final MessageArguments getDefaultErrorMsgArgs() {
		return defaultErrorMsgArgs;
	}

	/**
	 * Gets the 'requires a player' error message.
	 * <p>
	 * When overriding this method, consider using {@link #getDefaultErrorMsgArgs()} for the common
	 * message arguments.
	 * 
	 * @return the error message
	 */
	public Text getRequiresPlayerErrorMsg() {
		Text text = Messages.commandArgumentRequiresPlayer;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		return text;
	}

	/**
	 * This creates a {@link ArgumentParseException} with the 'requires a player' error message.
	 * 
	 * @return the exception
	 * @see #getRequiresPlayerErrorMsg()
	 */
	protected final RequiresPlayerArgumentException requiresPlayerError() {
		return new RequiresPlayerArgumentException(this, this.getRequiresPlayerErrorMsg());
	}

	/**
	 * Gets the 'missing argument' error message.
	 * <p>
	 * When overriding this method, consider using {@link #getDefaultErrorMsgArgs()} for the common
	 * message arguments.
	 * 
	 * @return the error message, not <code>null</code>
	 */
	public Text getMissingArgumentErrorMsg() {
		Text text = Messages.commandArgumentMissing;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		return text;
	}

	/**
	 * This creates a {@link MissingArgumentException} with the 'missing argument' error message.
	 * 
	 * @return the exception, not <code>null</code>
	 * @see #getMissingArgumentErrorMsg()
	 */
	public final MissingArgumentException missingArgumentError() {
		return new MissingArgumentException(this, this.getMissingArgumentErrorMsg());
	}

	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.commandArgumentInvalid;
	}

	/**
	 * Gets the 'invalid argument' error message.
	 * <p>
	 * When overriding this method, consider using {@link #getInvalidArgumentErrorMsgText()} for the
	 * text template, and {@link #getDefaultErrorMsgArgs()} for the common message arguments.
	 * 
	 * @param argumentInput
	 *            the argument input, not <code>null</code>
	 * @return the error message, not <code>null</code>
	 */
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		Validate.notNull(argumentInput, "argumentInput is null");
		Text text = this.getInvalidArgumentErrorMsgText();
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments("argument", argumentInput);
		return text;
	}

	/**
	 * This creates an {@link InvalidArgumentException} with the 'invalid argument' error message.
	 * 
	 * @param argumentInput
	 *            the invalid argument input, not <code>null</code>
	 * @return the exception, not <code>null</code>
	 * @see #getInvalidArgumentErrorMsg(String)
	 */
	public final InvalidArgumentException invalidArgumentError(String argumentInput) {
		return new InvalidArgumentException(this, this.getInvalidArgumentErrorMsg(argumentInput));
	}

	/**
	 * Parses a value for this argument and stores it in the {@link CommandContext} with the
	 * argument's name as key.
	 * <p>
	 * This may also store other additional values inside the context, but it is recommended to use
	 * keys that don't conflict with any other command arguments, such as keys prefixed with the
	 * argument's name: <code>"{argumentName}:{key}"</code>.
	 * <p>
	 * This moves the cursor of the {@link ArgumentsReader} forward for all used up arguments. If
	 * parsing fails, the state of the {@link ArgumentsReader} is undefined (it is left to the
	 * caller to reset it if required).
	 * <p>
	 * By default this delegates the parsing to
	 * {@link #parseValue(CommandInput, CommandContextView, ArgumentsReader)}.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the context which stores the parsed argument values, not <code>null</code>
	 * @param argsReader
	 *            the {@link ArgumentsReader}, not <code>null</code>
	 * @return the parsed value, or <code>null</code> if nothing was parsed (e.g. for optional
	 *         command arguments)
	 * @throws ArgumentParseException
	 *             if unable to parse a value for a non-optional command argument
	 */
	public T parse(
			CommandInput input,
			CommandContext context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// Throws an ArgumentParseException on failure:
		T value = this.parseValue(input, context.getView(), argsReader);
		if (value != null) {
			context.put(name, value);
		}
		// TODO Cast suppresses a CheckerFramework false positive
		return Unsafe.cast(value);
	}

	/**
	 * Parses a value for this argument from the given {@link ArgumentsReader} and moves its cursor
	 * forward for every used-up argument.
	 * <p>
	 * The parsing may depend on contents of the passed {@link CommandContext}, but unlike
	 * {@link #parse(CommandInput, CommandContext, ArgumentsReader)} this method is not allowed to
	 * modify the context.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            an unmodifiable view on the context storing the already parsed argument values,
	 *            not <code>null</code>
	 * @param argsReader
	 *            the ArgumentsReader, not <code>null</code>
	 * @return the parsed value, or <code>null</code> if nothing was parsed (e.g. for optional
	 *         command arguments)
	 * @throws ArgumentParseException
	 *             if unable to parse a value for a non-optional command argument
	 */
	public abstract T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException;

	/**
	 * This provides completion suggestions for the last (possibly partial or empty) argument, IF
	 * parsing this argument would use up the last argument of the {@link ArgumentsReader}.
	 * <p>
	 * Don't expect the returned suggestions list to be mutable. However, <code>null</code> is not a
	 * valid return value, neither for the suggestions list nor its contents.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            an unmodifiable view on the context storing the already parsed argument values,
	 *            not <code>null</code>
	 * @param argsReader
	 *            the ArgumentsReader, not <code>null</code>
	 * @return the suggestions for the final argument, or an empty list to indicate 'no suggestions'
	 *         (not <code>null</code> and not containing <code>null</code>)
	 */
	public abstract List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommandArgument [name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	// Note on hashCode and equals: CommandArguments are compared by identity.

	// Factories for common derived argument types:

	public final CommandArgument<@Nullable T> optional() {
		return new OptionalArgument<T>(Unsafe.initialized(this));
	}

	public final CommandArgument<T> orDefaultValue(T defaultValue) {
		return new DefaultValueFallback<>(Unsafe.initialized(this), defaultValue);
	}
}
