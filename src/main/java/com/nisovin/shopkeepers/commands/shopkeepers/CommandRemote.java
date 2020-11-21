package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;

class CommandRemote extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_PLAYER = "player";

	CommandRemote() {
		super("remote", Arrays.asList("open"));

		// Set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionRemote);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true),
				TargetShopkeeperFilter.ANY
		));
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		assert targetPlayer != null;
		if (targetPlayer != sender) {
			this.checkPermission(sender, ShopkeepersPlugin.REMOTE_OTHER_PLAYERS_PERMISSION);
		}

		// Open shop trading window:
		shopkeeper.openTradingWindow(targetPlayer);
	}
}
