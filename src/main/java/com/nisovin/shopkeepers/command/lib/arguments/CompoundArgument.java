package com.nisovin.shopkeepers.command.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandArgument;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;

/**
 * An abstract {@link CommandArgument} which consists of multiple other command arguments which it parses its value
 * from.
 */
public abstract class CompoundArgument extends CommandArgument {

	public static final String FORMAT_DELIMITER = " ";

	private final List<CommandArgument> arguments;
	private final boolean useReducedFormat;
	private final String reducedFormat;

	public CompoundArgument(String name, List<CommandArgument> arguments) {
		this(name, arguments, true);
	}

	public CompoundArgument(String name, List<CommandArgument> arguments, boolean joinFormats) {
		this(name, arguments, joinFormats, true);
	}

	public CompoundArgument(String name, List<CommandArgument> arguments, boolean joinFormats, boolean useReducedFormat) {
		super(name);

		// arguments:
		Validate.notNull(arguments);
		List<CommandArgument> argumentsList = new ArrayList<>(arguments.size());
		this.arguments = Collections.unmodifiableList(argumentsList);
		for (CommandArgument argument : arguments) {
			Validate.notNull(argument);
			argumentsList.add(argument);
		}
		Validate.isTrue(this.arguments.size() != 0, "No (valid) arguments given!");

		// format:
		// the reduced format can only be used in conjunction with joinFormats:
		this.useReducedFormat = joinFormats ? useReducedFormat : false;
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			for (CommandArgument argument : this.arguments) {
				// appending full format for child-arguments here:
				format.append(argument.getFormat()).append(delimiter);
			}
			this.reducedFormat = format.substring(0, format.length() - delimiter.length());
		} else {
			// using the default format:
			this.reducedFormat = super.getReducedFormat();
		}
	}

	public List<CommandArgument> getArguments() {
		return arguments;
	}

	@Override
	public boolean isOptional() {
		// this argument is optional, if all child-arguments are optional:
		for (CommandArgument argument : arguments) {
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
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		// parse requirements:
		CommandContext localContext = new CommandContext();
		for (CommandArgument argument : arguments) {
			argument.parse(input, localContext, args);
		}

		// parse actual value for this compound argument:
		return this.parseValue(input, localContext, args);
	}

	protected abstract Object parseValue(CommandInput input, CommandContext localContext, CommandArgs args) throws ArgumentParseException;

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		CommandContext localContext = new CommandContext();
		for (CommandArgument argument : arguments) {
			Object state = args.getState();
			try {
				argument.parse(input, localContext, args);
				// successfully parsed:
				if (!args.hasNext()) {
					// this consumed the last argument:
					// reset args and provide alternative completions for the last argument instead:
					args.setState(state);
					return argument.complete(input, localContext, args);
				}
			} catch (ArgumentParseException e) {
				// parsing might have failed because of invalid partial last argument:
				// -> return suggestions
				// parsing might also have failed because of invalid argument inside the sequence of arguments:
				// -> skip later arguments, and no suggestions are returned, because current argument isn't using up the
				// last argument in the given sequence
				args.setState(state);
				return argument.complete(input, localContext, args);
			}
		}
		return Collections.emptyList();
	}
}
