package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandSetTradePerm extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_NEW_PERMISSION = "perm";
	private static final String ARGUMENT_REMOVE_PERMISSION = "-";
	private static final String ARGUMENT_QUERY_PERMISSION = "?";

	CommandSetTradePerm() {
		super("setTradePerm");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SET_TRADE_PERM_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSettradeperm);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.ADMIN
								.and(ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR()))),
				TargetShopkeeperFilter.ADMIN
		));
		this.addArgument(new FirstOfArgument("permArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY_PERMISSION)
						.orDefaultValue(ARGUMENT_QUERY_PERMISSION),
				new LiteralArgument(ARGUMENT_REMOVE_PERMISSION),
				new StringArgument(ARGUMENT_NEW_PERMISSION)
		), true, true));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		AbstractAdminShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);

		// Check that the sender can edit this shop:
		if (!shopkeeper.canEdit(sender, false)) {
			return;
		}

		String newTradePerm = context.getOrNull(ARGUMENT_NEW_PERMISSION);
		boolean removePerm = context.has(ARGUMENT_REMOVE_PERMISSION);
		String currentTradePerm = shopkeeper.getTradePermission();
		if (currentTradePerm == null) currentTradePerm = "-";

		if (removePerm) {
			// Remove trade permission:
			assert newTradePerm == null;
			TextUtils.sendMessage(sender, Messages.tradePermRemoved, "perm", currentTradePerm);
		} else if (newTradePerm != null) {
			// Set trade permission:
			TextUtils.sendMessage(sender, Messages.tradePermSet, "perm", newTradePerm);
		} else {
			// Display current trade permission:
			TextUtils.sendMessage(sender, Messages.tradePermView, "perm", currentTradePerm);
			return;
		}

		// Set trade permission:
		shopkeeper.setTradePermission(newTradePerm);

		// Save:
		shopkeeper.save();
	}
}
