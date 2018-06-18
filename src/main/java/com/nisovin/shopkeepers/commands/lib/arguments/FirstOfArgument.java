package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Pair;

/**
 * A {@link CommandArgument} which stores the value for the first of the given command arguments which successfully
 * parses something.
 * <p>
 * If an argument provides <code>null</code> as value, parsing continues, giving the remaining arguments a chance to
 * parse something 'more useful'. However, no {@link ArgumentParseException} will be thrown in this case if no argument
 * parses a non-null value.
 */
public class FirstOfArgument extends CommandArgument {

	public static final String FORMAT_DELIMITER = "|";

	private final List<CommandArgument> arguments;
	private final String reducedFormat;

	public FirstOfArgument(String name, List<CommandArgument> arguments) {
		this(name, arguments, true, false);
	}

	public FirstOfArgument(String name, List<CommandArgument> arguments, boolean joinFormats) {
		this(name, arguments, joinFormats, false);
	}

	public FirstOfArgument(String name, List<CommandArgument> arguments, boolean joinFormats, boolean reverseFormat) {
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
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			ListIterator<CommandArgument> iterator = this.arguments.listIterator(reverseFormat ? this.arguments.size() : 0);
			while (reverseFormat ? iterator.hasPrevious() : iterator.hasNext()) {
				CommandArgument argument = (reverseFormat ? iterator.previous() : iterator.next());
				// appending reduced format for child-arguments here:
				format.append(argument.getReducedFormat()).append(delimiter);
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
	public String getReducedFormat() {
		return reducedFormat;
	}

	@Override
	public void parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		Pair<CommandArgument, Object> result;
		try {
			result = this.parseValue(input, args);
		} catch (ArgumentParseException e) {
			// restoring previous args state:
			args.setState(state);

			if (this.isOptional()) {
				// set value to null:
				result = null;
			} else {
				// pass on exception:
				throw e;
			}
		}
		if (result != null) {
			context.put(this.getName(), result);

			// store the parsed result under the parsed argument's name:
			CommandArgument argument = result.getFirst();
			Object value = result.getSecond();
			if (argument != null && value != null) {
				context.put(argument.getName(), value);
			}
		}
	}

	// returns a pair with the argument and the parsed value, or null if nothing was parsed
	@Override
	public Pair<CommandArgument, Object> parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		Object state = args.getState();

		// try one after the other:
		Object value = null;
		boolean nullParsed = false;
		for (CommandArgument argument : arguments) {
			try {
				value = argument.parseValue(input, args);
				if (value != null) {
					// we successfully parsed something:
					return Pair.of(argument, value);
				} else {
					nullParsed = true;
					// continue: maybe some other argument can parse something more useful..
				}
			} catch (ArgumentParseException e) {
				// ignore
			}
			// reset state and continue:
			args.setState(state);
		}

		if (nullParsed) {
			// if one argument did return null as parsing result, and did not
			// throw an exception (like optional argument tend to do), we don't
			// throw an exception here either:
			return null;
		}

		// invalid argument for all of them:
		throw this.invalidArgument(args.peek());
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		List<String> suggestions = new ArrayList<>();
		for (CommandArgument argument : arguments) {
			suggestions.addAll(argument.complete(input, context, args));
		}
		return suggestions;
	}
}
