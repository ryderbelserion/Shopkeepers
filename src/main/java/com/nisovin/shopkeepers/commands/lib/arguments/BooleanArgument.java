package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class BooleanArgument extends CommandArgument<Boolean> {

	public BooleanArgument(String name) {
		super(name);
	}

	@Override
	public Boolean parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		Boolean value = ConversionUtils.parseBoolean(argument);
		if (value == null) {
			throw this.invalidArgumentError(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		String partialArg = argsReader.next().toLowerCase();
		for (String value : ConversionUtils.BOOLEAN_VALUES.keySet()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (value.toLowerCase().startsWith(partialArg)) {
				suggestions.add(value);
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
