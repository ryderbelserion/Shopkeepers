package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;

/**
 * Parses an UUID.
 * <p>
 * Provides no completions.
 */
public class UUIDArgument extends CommandArgument<UUID> {

	public UUIDArgument(String name) {
		super(name);
	}

	// TODO 'invalid uuid' message

	@Override
	public UUID parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		String uuidArg = args.nextIfPresent();
		if (uuidArg == null) {
			throw this.missingArgument();
		} else {
			UUID uuid = ConversionUtils.parseUUID(uuidArg);
			if (uuid == null) {
				throw this.invalidArgument(uuidArg);
			} else {
				return uuid;
			}
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return Collections.emptyList();
	}
}
