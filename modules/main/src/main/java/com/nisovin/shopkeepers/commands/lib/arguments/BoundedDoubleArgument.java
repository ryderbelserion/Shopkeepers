package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.Validate;

public class BoundedDoubleArgument extends DoubleArgument {

	private final double min;
	private final double max;

	public BoundedDoubleArgument(String name, double min, double max) {
		super(name);
		Validate.isTrue(min <= max, "min > max");
		this.min = min;
		this.max = max;
	}

	// TODO More descriptive error messages

	@Override
	public Double parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		Double value = super.parseValue(input, context, argsReader);
		assert value != null;
		if (value < min || value > max) {
			throw this.invalidArgumentError(argsReader.current());
		}
		return value;
	}
}
