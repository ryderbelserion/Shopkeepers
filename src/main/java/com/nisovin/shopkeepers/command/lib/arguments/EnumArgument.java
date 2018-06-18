package com.nisovin.shopkeepers.command.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandArgument;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class EnumArgument<T extends Enum<T>> extends CommandArgument {

	private final Class<T> clazz;

	public EnumArgument(String name, Class<T> clazz) {
		super(name);
		Validate.notNull(clazz);
		this.clazz = clazz;
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		String argument = args.next();
		T value = ConversionUtils.parseEnum(clazz, argument);
		if (value == null) {
			throw this.invalidArgument(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = args.next().toUpperCase();
			for (T value : clazz.getEnumConstants()) {
				if (value.name().toUpperCase().startsWith(partialArg)) {
					suggestions.add(value.name());
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
