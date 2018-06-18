package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

/**
 * Wraps a given {@link CommandArgument} and makes it optional.
 * 
 * <p>
 * See also {@link CommandArgument#isOptional()}.
 */
public class OptionalArgument extends CommandArgument {

	protected final CommandArgument argument;

	public OptionalArgument(CommandArgument argument) {
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
			// restoring previous args state:
			args.setState(state);
		}
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		return argument.parseValue(input, args);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return argument.complete(input, context, args);
	}
}
