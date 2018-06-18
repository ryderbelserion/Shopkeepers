package com.nisovin.shopkeepers.command.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandException;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.command.shopkeepers.arguments.ShopkeeperArgument;

class CommandRemote extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandRemote() {
		super(Arrays.asList("remote"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemote);

		// arguments:
		this.addArgument(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true, (shopkeeper) -> shopkeeper instanceof PlayerShopkeeper));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null && shopkeeper instanceof PlayerShopkeeper;

		// open shop trading window:
		shopkeeper.openTradingWindow(player);
	}
}
