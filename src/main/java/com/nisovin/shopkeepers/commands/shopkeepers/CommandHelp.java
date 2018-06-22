package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.HelpCommand;

class CommandHelp extends HelpCommand {

	CommandHelp(Command helpSource) {
		super("help", Arrays.asList("?"), helpSource);

		// set permission:
		this.setPermission(ShopkeepersPlugin.HELP_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionHelp);
	}
}
