package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.List;

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
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;

class CommandRemote extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandRemote() {
		super("remote", Arrays.asList("open"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemote);

		// arguments:
		this.addArgument(new OptionalArgument(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true)));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		if (shopkeeper == null) {
			// get shopkeeper via targeting:
			List<? extends Shopkeeper> shopkeepers = ShopkeeperUtils.getTargetedShopkeepers(player, null, true);
			if (shopkeepers.isEmpty()) return; // messages were already handled
			shopkeeper = shopkeepers.get(0); // use the first returned shopkeeper
		}
		assert shopkeeper != null;

		// open shop trading window:
		shopkeeper.openTradingWindow(player);
	}
}
