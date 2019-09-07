package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class WorldArgument extends CommandArgument<World> {

	public WorldArgument(String name) {
		super(name);
	}

	@Override
	public World parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		String argument = args.next();
		World value = Bukkit.getWorld(argument);
		if (value == null) {
			// interpret input as world id:
			UUID uuid = ConversionUtils.parseUUID(argument);
			if (uuid != null) {
				value = Bukkit.getWorld(uuid);
			}
			if (value == null) {
				throw this.invalidArgument(argument);
			}
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return Collections.emptyList();
	}
}
