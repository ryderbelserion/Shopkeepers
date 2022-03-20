package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShops;
import com.nisovin.shopkeepers.text.Text;

class CommandCleanupCitizenShopkeepers extends Command {

	CommandCleanupCitizenShopkeepers() {
		super("cleanupCitizenShopkeepers");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.CLEANUP_CITIZEN_SHOPKEEPERS);

		// Set description:
		this.setDescription(Text.of("Deletes invalid Citizen shopkeepers."));

		// Hidden utility command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		// Find and delete invalid Citizen shopkeepers:
		CitizensShops citizensShops = SKShopkeepersPlugin.getInstance().getCitizensShops();
		int deleted = citizensShops.validateCitizenShopkeepers(true, false);

		// Inform command executor:
		sender.sendMessage(ChatColor.GREEN + "Deleted " + ChatColor.YELLOW + deleted
				+ ChatColor.GREEN + " invalid Citizen shopkeepers!");
	}
}
