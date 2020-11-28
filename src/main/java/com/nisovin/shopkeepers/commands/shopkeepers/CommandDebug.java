package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.config.Settings;

class CommandDebug extends Command {

	CommandDebug() {
		super("debug");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionDebug);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		// Toggle debug mode:
		Settings.debug = !Settings.debug;
		Settings.onSettingsChanged();
		sender.sendMessage(ChatColor.GREEN + "Debug mode " + (Settings.debug ? "enabled" : "disabled"));
	}
}
