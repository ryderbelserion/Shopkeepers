package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class EnumArgument<T extends Enum<T>> extends CommandArgument<@NonNull T> {

	private final Class<@NonNull T> clazz;

	public EnumArgument(String name, Class<@NonNull T> clazz) {
		super(name);
		Validate.notNull(clazz, "clazz is null");
		this.clazz = clazz;
	}

	@Override
	public T parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		@Nullable T value = ConversionUtils.parseEnum(clazz, argument);
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
		String partialArg = argsReader.next().toUpperCase();
		@NonNull T[] enumValues = Unsafe.assertNonNull(clazz.getEnumConstants());
		for (T value : enumValues) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (value.name().toUpperCase().startsWith(partialArg)) {
				suggestions.add(value.name());
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
