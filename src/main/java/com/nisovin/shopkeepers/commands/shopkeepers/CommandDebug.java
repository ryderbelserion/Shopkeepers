package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

class CommandDebug extends Command {

	CommandDebug() {
		super("debug");

		// set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionDebug);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		CommandSender sender = input.getSender();

		// toggle debug mode:
		Settings.debug = !Settings.debug;
		sender.sendMessage(ChatColor.GREEN + "Debug mode " + (Settings.debug ? "enabled" : "disabled"));
	}
}
