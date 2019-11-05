package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.SimpleCommandContext;
import com.nisovin.shopkeepers.util.Validate;

/**
 * An abstract {@link CommandArgument} which consists of multiple other command arguments which it parses its value
 * from.
 */
public abstract class CompoundArgument<T> extends CommandArgument<T> {

	public static final String FORMAT_DELIMITER = " ";

	private final List<CommandArgument<?>> arguments;
	private final boolean useReducedFormat;
	private final String reducedFormat;

	public CompoundArgument(String name, List<CommandArgument<?>> arguments) {
		this(name, arguments, true);
	}

	public CompoundArgument(String name, List<CommandArgument<?>> arguments, boolean joinFormats) {
		this(name, arguments, joinFormats, true);
	}

	public CompoundArgument(String name, List<CommandArgument<?>> arguments, boolean joinFormats, boolean useReducedFormat) {
		super(name);

		// arguments:
		Validate.notNull(arguments, "Arguments is null!");
		List<CommandArgument<?>> argumentsList = new ArrayList<>(arguments.size());
		this.arguments = Collections.unmodifiableList(argumentsList);
		for (CommandArgument<?> argument : arguments) {
			Validate.notNull(argument, "A contained argument is null!");
			// TODO this also excludes optional arguments.. allow fallbacks and handle them somehow? Maybe evaluate
			// fallbacks immediately?
			Validate.isTrue(!(argument instanceof FallbackArgument), "Cannot use fallback arguments in compound argument!");
			argument.setParent(this);
			argumentsList.add(argument);
		}
		Validate.isTrue(this.arguments.size() != 0, "No arguments given!");

		// format:
		// the reduced format can only be used in conjunction with joinFormats:
		this.useReducedFormat = joinFormats ? useReducedFormat : false;
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			for (CommandArgument<?> argument : this.arguments) {
				// appending full format for child-arguments here:
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
			// using the default format:
			this.reducedFormat = super.getReducedFormat();
		}
	}

	public List<CommandArgument<?>> getArguments() {
		return arguments;
	}

	@Override
	public boolean isOptional() {
		// this argument is optional, if all child-arguments are optional:
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
		return reducedFormat;
	}

	// This is not throwing this compound-argument's 'missing argument' exception by default,
	// because the first (non-optional) missing child argument will already throw its exceptions
	// with a more specific message.
	// If a different behavior is wanted, the parseValue method can be overridden to throw the
	// compound-argument's exception instead.

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// parse requirements:
		CommandContext localContext = context.copy();
		for (CommandArgument<?> argument : arguments) {
			argument.parse(input, localContext, argsReader);
		}

		// parse actual value for this compound argument:
		return this.parseCompoundValue(input, localContext.getView(), argsReader);
	}

	protected abstract T parseCompoundValue(CommandInput input, CommandContextView localContext, ArgumentsReader args) throws ArgumentParseException;

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		List<String> suggestions = new ArrayList<>();
		CommandContext localContext = new SimpleCommandContext();
		CommandContextView localContextView = localContext.getView();
		// Similar to Command#handleTabCompletion
		for (CommandArgument<?> argument : arguments) {
			int remainingArgs = argsReader.getRemainingSize();
			if (remainingArgs == 0) {
				// no argument left which could be completed:
				break;
			}
			ArgumentsReader argsReaderState = argsReader.createSnapshot();
			try {
				argument.parse(input, localContext, argsReader);
				// successfully parsed:
				if (!argsReader.hasNext()) {
					// this consumed the last argument:
					// reset args and provide alternative completions for the last argument instead:
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, localContextView, argsReader));
					break;
				} else if (argsReader.getRemainingSize() == remainingArgs) {
					// no error during parsing, but none of the remaining args used up:
					// -> this was an optional argument which got skipped
					// include suggestions (if it has any), but continue:
					suggestions.addAll(argument.complete(input, localContextView, argsReader));

					// reset state (just in case), and then let the following arguments also try to complete the same
					// arg(s):
					argsReader.setState(argsReaderState);
					continue;
				}
			} catch (ArgumentParseException e) {
				if (argsReader.getRemainingSize() == remainingArgs) {
					// error, but none of the remaining args were used up:
					// -> this was a hidden argument that didn't consume any arguments
					// -> skip and continue
					// reset state (just in case):
					argsReader.setState(argsReaderState);
					continue;
				} else {
					// parsing might have failed because of an invalid partial last argument
					// -> include suggestions in that case
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, localContextView, argsReader));
					// parsing might also have failed because of an invalid argument inside the sequence of arguments
					// -> skip later arguments (current argument will not provide suggestions in that case, because it
					// isn't using up the last argument)
					break;
				}
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
