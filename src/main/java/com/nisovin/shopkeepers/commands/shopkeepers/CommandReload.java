package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

class CommandReload extends Command {

	private final SKShopkeepersPlugin plugin;

	CommandReload(SKShopkeepersPlugin plugin) {
		super("reload");
		this.plugin = plugin;

		// set permission:
		this.setPermission(ShopkeepersPlugin.RELOAD_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionReload);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		CommandSender sender = input.getSender();

		// reload plugin:
		plugin.reload();
		sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
	}
}
