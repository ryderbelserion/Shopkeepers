package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class LiteralArgument extends CommandArgument<String> {

	public static final String FORMAT_PREFIX = "'";
	public static final String FORMAT_SUFFIX = "'";

	private final List<String> literals; // not null nor containing null or empty literals

	public LiteralArgument(String name) {
		this(name, null);
	}

	public LiteralArgument(String name, List<String> aliases) {
		super(name);
		Validate.isTrue(!StringUtils.containsWhitespace(name), "LiteralArgument name contains whitespace!");

		// initialize literals:
		this.literals = new ArrayList<>(1 + (aliases == null ? 0 : aliases.size()));
		literals.add(name);
		if (aliases != null) {
			for (String alias : aliases) {
				if (alias == null) continue;
				alias = StringUtils.removeWhitespace(alias);
				if (alias.isEmpty()) continue;
				literals.add(alias);
			}
		}
	}

	@Override
	public String getReducedFormat() {
		return FORMAT_PREFIX + this.getName() + FORMAT_SUFFIX;
	}

	@Override
	public String parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}

		String argument = argsReader.next();
		String value = null;
		for (String literal : literals) {
			if (argument.equalsIgnoreCase(literal)) {
				value = literal;
				break;
			}
		}
		if (value == null) {
			throw this.invalidArgumentError(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = argsReader.next().toLowerCase();
			for (String literal : literals) {
				if (suggestions.size() >= MAX_SUGGESTIONS) break;
				if (literal.toLowerCase().startsWith(partialArg)) {
					suggestions.add(literal);
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
