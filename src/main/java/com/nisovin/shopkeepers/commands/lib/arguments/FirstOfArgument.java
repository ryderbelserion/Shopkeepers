package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.util.Pair;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link CommandArgument} which stores the value for the first of the given command arguments which successfully
 * parses something.
 * <p>
 * If an argument provides <code>null</code> as value, parsing continues, giving the remaining arguments a chance to
 * parse something 'more useful'. However, no {@link ArgumentParseException} will be thrown in this case if no argument
 * parses a non-null value.
 */
public class FirstOfArgument extends CommandArgument<Pair<CommandArgument<?>, Object>> {

	public static final String FORMAT_DELIMITER = "|";

	private final List<CommandArgument<?>> arguments;
	private final String reducedFormat;

	public FirstOfArgument(String name, List<CommandArgument<?>> arguments) {
		this(name, arguments, true, false);
	}

	public FirstOfArgument(String name, List<CommandArgument<?>> arguments, boolean joinFormats) {
		this(name, arguments, joinFormats, false);
	}

	public FirstOfArgument(String name, List<CommandArgument<?>> arguments, boolean joinFormats, boolean reverseFormat) {
		super(name);

		// arguments:
		Validate.notNull(arguments, "Arguments is null!");
		List<CommandArgument<?>> argumentsList = new ArrayList<>(arguments.size());
		this.arguments = Collections.unmodifiableList(argumentsList);
		for (CommandArgument<?> argument : arguments) {
			Validate.notNull(argument, "Contained argument is null!");
			argument.setParent(this);
			argumentsList.add(argument);
		}
		Validate.isTrue(this.arguments.size() != 0, "No arguments given!");

		// format:
		if (joinFormats) {
			String delimiter = FORMAT_DELIMITER;
			StringBuilder format = new StringBuilder();
			ListIterator<CommandArgument<?>> iterator = this.arguments.listIterator(reverseFormat ? this.arguments.size() : 0);
			while (reverseFormat ? iterator.hasPrevious() : iterator.hasNext()) {
				CommandArgument<?> argument = (reverseFormat ? iterator.previous() : iterator.next());
				// appending reduced format for child-arguments here:
				String argumentFormat = argument.getReducedFormat();
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
		// this argument is optional, if at least one child-argument is optional:
		for (CommandArgument<?> argument : arguments) {
			if (argument.isOptional()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getReducedFormat() {
		return reducedFormat;
	}

	@Override
	public Pair<CommandArgument<?>, Object> parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		Pair<CommandArgument<?>, Object> result;
		try {
			// with context, so that the child argument can store its value(s)
			result = this.parseValue(input, context, args);
		} catch (ArgumentParseException e) {
			// restore previous args state:
			args.setState(state);
			throw e;
		}
		if (result != null) {
			context.put(this.getName(), result);
		}
		return result;
	}

	// returns a pair with the argument and the parsed value, or null if nothing was parsed
	@Override
	public Pair<CommandArgument<?>, Object> parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		return this.parseValue(input, null, args); // without context
	}

	// context != null: use 'parse' rather than 'parseValue' in order to let the child argument store its parsed value
	private Pair<CommandArgument<?>, Object> parseValue(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		// try one after the other:
		Object state = args.getState();
		Object value = null;
		FallbackArgumentException fallbackException = null;
		boolean nullParsed = false;
		ArgumentRejectedException rejectedException = null;
		ArgumentParseException firstParseException = null;
		for (CommandArgument<?> argument : arguments) {
			try {
				if (context != null) {
					value = argument.parse(input, context, args);
				} else {
					value = argument.parseValue(input, args);
				}
				if (value != null) {
					// we successfully parsed something:
					return Pair.of(argument, value);
				} else {
					nullParsed = true;
					// continue: maybe some other argument can parse something more useful..
				}
			} catch (FallbackArgumentException e) {
				// ignore, but keep track of the first fallback exception:
				if (fallbackException == null) {
					fallbackException = e;
				}
			} catch (ArgumentRejectedException e) {
				// ignore, but keep track of the first argument-rejected exception:
				if (rejectedException == null) {
					rejectedException = e;
				}
			} catch (ArgumentParseException e) {
				// ignore, but keep track of the first exception:
				if (firstParseException == null) {
					firstParseException = e;
				}
			}
			// reset state and continue:
			args.setState(state);
		}

		if (fallbackException != null) {
			// if some argument might be able to provide a fallback, prefer following that path:
			// TODO but if the fallback turns out failing, we might want to prefer null, rejected or first exception
			// TODO there might also be more than one applicable fallback
			throw fallbackException;
		}

		if (nullParsed) {
			// if one argument did return null as parsing result, and did not throw an exception (like optional
			// arguments tend to do), we don't throw an exception here either:
			return null;
		}

		if (rejectedException != null) {
			// Some argument was able to parse something but rejected the result due to some filter.
			// Prefer this more specific exception since it is likely to be more relevant to the user.
			throw rejectedException;
		}

		// invalid argument for all of them:
		assert firstParseException != null; // otherwise we would have parsed something (or null)
		throw firstParseException;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		List<String> suggestions = new ArrayList<>();
		Object state = args.getState(); // keep track of the initial state
		for (CommandArgument<?> argument : arguments) {
			int limit = (MAX_SUGGESTIONS - suggestions.size());
			if (limit <= 0) break;

			// reset args so that every argument has a chance to provide completions:
			args.setState(state);

			List<String> argumentSuggestions = argument.complete(input, context, args);
			if (argumentSuggestions.size() < limit) {
				suggestions.addAll(argumentSuggestions);
			} else {
				suggestions.addAll(argumentSuggestions.subList(0, limit));
				break;
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
