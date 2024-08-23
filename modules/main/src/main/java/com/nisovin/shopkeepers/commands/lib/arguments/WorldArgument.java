package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.java.ConversionUtils;

public class WorldArgument extends CommandArgument<World> {

	public WorldArgument(String name) {
		super(name);
	}

	@Override
	public World parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		World value = Bukkit.getWorld(argument);
		if (value == null) {
			// Interpret input as world id:
			UUID uuid = ConversionUtils.parseUUID(argument);
			if (uuid != null) {
				value = Bukkit.getWorld(uuid);
			}
			if (value == null) {
				throw this.invalidArgumentError(argument);
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
		return Collections.emptyList();
	}
}
