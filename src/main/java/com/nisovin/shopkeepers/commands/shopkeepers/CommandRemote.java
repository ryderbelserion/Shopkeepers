package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;

class CommandRemote extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandRemote() {
		super("remote", Arrays.asList("open"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemote);

		// arguments:
		this.addArgument(new TargetShopkeeperFallback(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true), TargetShopkeeperFilter.ANY));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;

		// open shop trading window:
		shopkeeper.openTradingWindow(player);
	}
}
