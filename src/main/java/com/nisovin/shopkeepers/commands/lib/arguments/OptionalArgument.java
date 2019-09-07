package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps a given {@link CommandArgument} and makes it optional.
 * <p>
 * If no value can be parsed, no {@link ArgumentParseException} is thrown, but <code>null</code> is returned instead.
 */
public class OptionalArgument<T> extends CommandArgument<T> {

	protected final CommandArgument<T> argument;

	public OptionalArgument(CommandArgument<T> argument) {
		super(argument.getName());
		Validate.notNull(argument);
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
	public void parse(CommandInput input, CommandContext context, CommandArgs args) throws ArgumentParseException {
		Object state = args.getState();
		try {
			// let the wrapped argument handle the parsing:
			argument.parse(input, context, args);
		} catch (ArgumentParseException e) {
			assert this.isOptional();
			// restore previous args state:
			args.setState(state);
		}
	}

	@Override
	public T parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		return argument.parseValue(input, args);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return argument.complete(input, context, args);
	}
}
