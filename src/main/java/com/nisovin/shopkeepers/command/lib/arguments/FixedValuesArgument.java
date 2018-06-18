package com.nisovin.shopkeepers.command.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandArgument;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;

public class FixedValuesArgument extends CommandArgument {

	private final Map<String, Object> values;

	public FixedValuesArgument(String name, Map<String, Object> values) {
		super(name);
		Validate.notNull(values);
		this.values = values;
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		String argument = args.next();
		Object value = values.get(argument);
		if (value == null) {
			// try again with lower and upper case variants of the input:
			value = values.get(argument.toLowerCase());
			if (value == null) {
				value = values.get(argument.toUpperCase());
				if (value == null) {
					throw this.invalidArgument(argument);
				}
			}
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = args.next().toLowerCase();
			for (String valueKey : values.keySet()) {
				if (valueKey == null) continue;
				if (valueKey.toLowerCase().startsWith(partialArg)) {
					suggestions.add(valueKey);
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
