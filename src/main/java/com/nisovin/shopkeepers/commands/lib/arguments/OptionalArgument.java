package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps a given {@link CommandArgument} and makes it optional.
 * <p>
 * If no value can be parsed, no {@link ArgumentParseException} is thrown, but <code>null</code> is returned instead.
 */
public class OptionalArgument<T> extends CommandArgument<T> {

	// TODO implement as 'do-nothing'-fallback instead?
	// if an invalid argument is specified, it gets currently forwarded to the next command argument, leading to a
	// likely confusing/unrelated parsing error message
	// fallback would jump back and throw the original parsing error if the following command argument can't parse it

	protected final CommandArgument<T> argument;

	public OptionalArgument(CommandArgument<T> argument) {
		super(argument.getName());
		Validate.notNull(argument, "Argument is null!");
		argument.setParent(this);
		this.argument = argument;
	}

	@Override
	public boolean isOptional() {
		return true;
	}

	@Override
	public String getReducedFormat() {
		return argument.getReducedFormat();
	}

	@Override
	public T parse(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		T value;
		try {
			// let the wrapped argument handle the parsing:
			value = argument.parse(input, context, argsReader);
		} catch (ArgumentParseException e) {
			assert this.isOptional();
			value = null;
			// restore previous args reader state:
			argsReader.setState(argsReaderState);
		}
		return value;
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		return argument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return argument.complete(input, context, argsReader);
	}
}
