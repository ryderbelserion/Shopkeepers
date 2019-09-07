package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

public class BoundedIntegerArgument extends IntegerArgument {

	private final int min;
	private final int max;

	public BoundedIntegerArgument(String name, int min, int max) {
		super(name);
		Validate.isTrue(min <= max, "min > max!");
		this.min = min;
		this.max = max;
	}

	// TODO more descriptive error messages

	@Override
	public Integer parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		Integer value = super.parseValue(input, args);
		assert value != null;
		if (value < min || value > max) {
			throw this.invalidArgument(args.current());
		}
		return value;
	}
}
