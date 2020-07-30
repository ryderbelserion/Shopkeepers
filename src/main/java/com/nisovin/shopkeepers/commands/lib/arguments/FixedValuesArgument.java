package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

public class FixedValuesArgument extends CommandArgument<Object> {

	private final Map<String, Object> values;

	public FixedValuesArgument(String name, Map<String, Object> values) {
		super(name);
		Validate.notNull(values, "Values is null");
		Validate.isTrue(!values.containsKey(null), "Values cannot contain null key!");
		Validate.isTrue(!values.containsValue(null), "Values cannot contain null value!");
		this.values = values;
	}

	@Override
	public Object parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		Object value = values.get(argument);
		if (value == null) {
			// Try again with lower and upper case variants of the input:
			value = values.get(argument.toLowerCase(Locale.ROOT));
			if (value == null) {
				value = values.get(argument.toUpperCase(Locale.ROOT));
				if (value == null) {
					throw this.invalidArgumentError(argument);
				}
			}
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		String partialArg = argsReader.next().toLowerCase(Locale.ROOT);
		for (String valueKey : values.keySet()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (valueKey.toLowerCase(Locale.ROOT).startsWith(partialArg)) {
				suggestions.add(valueKey);
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
