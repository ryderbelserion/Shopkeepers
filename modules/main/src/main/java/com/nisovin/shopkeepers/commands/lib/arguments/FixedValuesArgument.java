package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class FixedValuesArgument extends CommandArgument<Object> {

	private final Map<? extends String, @NonNull ?> values;

	public FixedValuesArgument(String name, Map<? extends String, @NonNull ?> values) {
		super(name);
		Validate.notNull(values, "values is null");
		Validate.isTrue(!CollectionUtils.containsNull(values.keySet()),
				"values contains a null key");
		Validate.isTrue(!CollectionUtils.containsNull(values.values()),
				"values contains a null value");
		this.values = values;
	}

	@Override
	public Object parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
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
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
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
