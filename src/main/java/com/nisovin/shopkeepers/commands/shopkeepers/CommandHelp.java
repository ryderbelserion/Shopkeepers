package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.HelpCommand;

class CommandHelp extends HelpCommand {

	CommandHelp(Command helpSource) {
		super("help", Arrays.asList("?"), helpSource);

		// Set permission:
		this.setPermission(ShopkeepersPlugin.HELP_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionHelp);
	}
}
