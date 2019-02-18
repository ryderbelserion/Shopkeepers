package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

public class PositiveIntegerArgument extends IntegerArgument {

	public PositiveIntegerArgument(String name) {
		super(name);
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		Integer value = (Integer) super.parseValue(input, args);
		assert value != null;
		if (value <= 0) {
			throw this.invalidArgument(args.current());
		}
		return value;
	}
}
