package com.nisovin.shopkeepers.command.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandArgument;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class BooleanArgument extends CommandArgument {

	public BooleanArgument(String name) {
		super(name);
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		String argument = args.next();
		Boolean value = ConversionUtils.parseBoolean(argument);
		if (value == null) {
			throw this.invalidArgument(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = args.next().toLowerCase();
			for (String value : ConversionUtils.BOOLEAN_VALUES.keySet()) {
				if (value.toLowerCase().startsWith(partialArg)) {
					suggestions.add(value);
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
