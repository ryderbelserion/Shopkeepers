package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

	// Not null nor containing null or empty literals, combines the argument name and aliases:
	private final List<String> literals;

	public LiteralArgument(String name) {
		this(name, null);
	}

	public LiteralArgument(String name, List<String> aliases) {
		super(name);

		// Initialize literals:
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
	public CommandArgument<String> setDisplayName(String displayName) {
		if (displayName != null) {
			Validate.isTrue(literals.contains(displayName), "The specified display name does not match any of the literal argument's literals!");
		}
		return super.setDisplayName(displayName);
	}

	@Override
	public String getReducedFormat() {
		return FORMAT_PREFIX + this.getDisplayName() + FORMAT_SUFFIX;
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
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		String partialArg = argsReader.next().toLowerCase(Locale.ROOT);
		// Hiding the argument name if a different display name is used:
		boolean skipName = (!this.getName().equals(this.getDisplayName()));
		for (String literal : literals) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (skipName && literal.equals(this.getName())) continue;
			if (literal.toLowerCase(Locale.ROOT).startsWith(partialArg)) {
				suggestions.add(literal);
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
