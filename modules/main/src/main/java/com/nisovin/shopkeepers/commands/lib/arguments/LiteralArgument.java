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
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class LiteralArgument extends CommandArgument<String> {

	public static final String FORMAT_PREFIX = "'";
	public static final String FORMAT_SUFFIX = "'";

	// Not null nor containing null or empty literals, combines the argument name and aliases:
	private final List<String> literals;

	public LiteralArgument(String name) {
		this(name, Collections.emptyList());
	}

	public LiteralArgument(String name, List<? extends String> aliases) {
		super(name);

		// Initialize literals:
		this.literals = new ArrayList<>(aliases.size() + 1);
		literals.add(name);
		for (String alias : aliases) {
			Validate.notNull(alias, "alias is null");
			alias = StringUtils.removeWhitespace(alias);
			assert alias != null;
			if (alias.isEmpty()) continue;
			literals.add(alias);
		}
	}

	@Override
	public CommandArgument<String> setDisplayName(@Nullable String displayName) {
		if (displayName != null) {
			Validate.isTrue(literals.contains(displayName),
					"displayName does not match any of the literals of this LiteralArgument");
		}
		return super.setDisplayName(displayName);
	}

	@Override
	public String getReducedFormat() {
		return FORMAT_PREFIX + this.getDisplayName() + FORMAT_SUFFIX;
	}

	@Override
	public String parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
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
