package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;

class CommandRemote extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandRemote() {
		super("remote");

		// set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemote);

		// arguments:
		this.addArgument(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true, (shopkeeper) -> shopkeeper instanceof AdminShopkeeper));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null && shopkeeper instanceof AdminShopkeeper;

		// open shop trading window:
		shopkeeper.openTradingWindow(player);
	}
}
