package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.context.SimpleCommandContext;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An abstract {@link CommandArgument} which consists of multiple other command arguments which it
 * parses its value from.
 * 
 * @param <T>
 *            the type of the parsed argument
 */
public abstract class CompoundArgument<T> extends CommandArgument<T> {

	public static final String FORMAT_DELIMITER = " ";

	private final List<? extends CommandArgument<?>> arguments;
	private final boolean useReducedFormat;
	// Null if the default (parent) reduced format shall be used:
	private final @Nullable String reducedFormat;

	public CompoundArgument(String name, List<? extends CommandArgument<?>> arguments) {
		this(name, arguments, true);
	}

	public CompoundArgument(
			String name,
			List<? extends CommandArgument<?>> arguments,
			boolean joinFormats
	) {
		this(name, arguments, joinFormats, true);
	}

	public CompoundArgument(
			String name,
			List<? extends CommandArgument<?>> arguments,
			boolean joinFormats,
			boolean useReducedFormat
	) {
		super(name);

		// Arguments:
		Validate.notNull(arguments, "arguments is null");
		Validate.isTrue(!arguments.isEmpty(), "arguments is empty");
		List<CommandArgument<?>> argumentsList = new ArrayList<>(arguments.size());
		this.arguments = Collections.unmodifiableList(argumentsList);
		for (CommandArgument<?> argument : arguments) {
			Validate.notNull(argument, "arguments contains null");
			// TODO This also excludes optional arguments.. allow fallbacks and handle them somehow?
			// Maybe evaluate fallbacks immediately?
			Validate.isTrue(!(argument instanceof FallbackArgument),
					"arguments contains a FallbackArgument");
			argument.setParent(this);
			argumentsList.add(argument);
		}
		assert !this.arguments.isEmpty();

		// Format:
		// The reduced format can only be used in conjunction with joinFormats:
		this.useReducedFormat = joinFormats ? useReducedFormat : false;
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			for (CommandArgument<?> argument : this.arguments) {
				// Appending full format for child-arguments here:
				String argumentFormat = argument.getFormat();
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

	public List<? extends CommandArgument<?>> getArguments() {
		return arguments;
	}

	@Override
	public boolean isOptional() {
		// This argument is optional, if all child-arguments are optional:
		for (CommandArgument<?> argument : arguments) {
			if (!argument.isOptional()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getFormat() {
		return useReducedFormat ? this.getReducedFormat() : super.getFormat();
	}

	@Override
	public String getReducedFormat() {
		if (reducedFormat != null) {
			return reducedFormat;
		} else {
			return super.getReducedFormat();
		}
	}

	// This is not throwing this compound-argument's 'missing argument' exception by default,
	// because the first (non-optional) missing child argument will already throw its exceptions
	// with a more specific message.
	// If a different behavior is wanted, the parseValue method can be overridden to throw the
	// compound-argument's exception instead.

	@Override
	public T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// Parse requirements:
		CommandContext localContext = context.copy();
		for (CommandArgument<?> argument : arguments) {
			argument.parse(input, localContext, argsReader);
		}

		// Parse actual value for this compound argument:
		return this.parseCompoundValue(input, localContext.getView(), argsReader);
	}

	protected abstract T parseCompoundValue(
			CommandInput input,
			CommandContextView localContext,
			ArgumentsReader args
	) throws ArgumentParseException;

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		List<String> suggestions = new ArrayList<>();
		CommandContext localContext = new SimpleCommandContext();
		CommandContextView localContextView = localContext.getView();
		// Similar to Command#handleTabCompletion
		for (CommandArgument<?> argument : arguments) {
			int remainingArgs = argsReader.getRemainingSize();
			if (remainingArgs == 0) {
				// No argument left which could be completed:
				break;
			}
			ArgumentsReader argsReaderState = argsReader.createSnapshot();
			try {
				argument.parse(input, localContext, argsReader);
				// Successfully parsed:
				if (!argsReader.hasNext()) {
					// This consumed the last argument:
					// Reset args and provide alternative completions for the last argument instead:
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, localContextView, argsReader));
					break;
				} else if (argsReader.getRemainingSize() == remainingArgs) {
					// No error during parsing, but none of the remaining args used up:
					// -> This was an optional argument which got skipped.
					// Include suggestions (if it has any), but continue:
					suggestions.addAll(argument.complete(input, localContextView, argsReader));

					// Reset state (just in case), and then let the following arguments also try to
					// complete the same arg(s):
					argsReader.setState(argsReaderState);
					continue;
				}
			} catch (ArgumentParseException e) {
				if (argsReader.getRemainingSize() == remainingArgs) {
					// Error, but none of the remaining args were used up:
					// -> This was a hidden argument that didn't consume any arguments.
					// -> Skip and continue.
					// Reset state (just in case):
					argsReader.setState(argsReaderState);
					continue;
				} else {
					// Parsing might have failed because of an invalid partial last argument.
					// -> Include suggestions in that case.
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, localContextView, argsReader));
					// Parsing might also have failed because of an invalid argument inside the
					// sequence of arguments.
					// -> Skip later arguments (current argument will not provide suggestions in
					// that case, because it isn't using up the last argument).
					break;
				}
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
