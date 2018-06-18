package com.nisovin.shopkeepers.command.shopkeepers;

import java.util.Arrays;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.command.lib.Command;
import com.nisovin.shopkeepers.command.lib.HelpCommand;

class CommandHelp extends HelpCommand {

	CommandHelp(Command helpSource) {
		super(helpSource, Arrays.asList("help", "?"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.HELP_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionHelp);
	}
}
