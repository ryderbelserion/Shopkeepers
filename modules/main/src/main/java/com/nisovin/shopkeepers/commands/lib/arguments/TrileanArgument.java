package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Trilean;

public class TrileanArgument extends CommandArgument<Trilean> {

	public TrileanArgument(String name) {
		super(name);
	}

	@Override
	public Trilean parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		@Nullable Trilean value = ConversionUtils.parseTrilean(argument);
		if (value == null) {
			throw this.invalidArgumentError(argument);
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
		for (String value : ConversionUtils.TRILEAN_VALUES.keySet()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			// The keys are already lowercase:
			if (value.startsWith(partialArg)) {
				suggestions.add(value);
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
