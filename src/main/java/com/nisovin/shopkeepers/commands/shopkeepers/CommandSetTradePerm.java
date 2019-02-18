package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.commands.arguments.AdminShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.shopkeeper.ShopTypeCategory;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;
import com.nisovin.shopkeepers.util.Utils;

class CommandSetTradePerm extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_NEW_PERMISSION = "perm";
	private static final String ARGUMENT_REMOVE_PERMISSION = "-";
	private static final String ARGUMENT_QUERY_PERMISSION = "?";

	CommandSetTradePerm() {
		super("setTradePerm");

		// set permission:
		this.setPermission(ShopkeepersPlugin.SET_TRADE_PERM_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionSettradeperm);

		// arguments:
		this.addArgument(new OptionalArgument(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, new AdminShopkeeperFilter())));
		this.addArgument(new OptionalArgument(new FirstOfArgument("permArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY_PERMISSION),
				new LiteralArgument(ARGUMENT_REMOVE_PERMISSION),
				new StringArgument(ARGUMENT_NEW_PERMISSION)), true, true)));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper == null || (shopkeeper instanceof AdminShopkeeper);
		if (shopkeeper == null) {
			// get shopkeeper via targeting:
			List<? extends Shopkeeper> shopkeepers = ShopkeeperUtils.getTargetedShopkeepers(player, ShopTypeCategory.ADMIN, true);
			if (shopkeepers.isEmpty()) return; // messages were already handled
			shopkeeper = shopkeepers.get(0);
		}

		String newTradePerm = context.get(ARGUMENT_NEW_PERMISSION);
		boolean removePerm = context.has(ARGUMENT_REMOVE_PERMISSION);
		String currentTradePerm = ((AdminShopkeeper) shopkeeper).getTradePremission();
		if (currentTradePerm == null) currentTradePerm = "-";

		if (removePerm) {
			// remove trade permission:
			assert newTradePerm == null;
			Utils.sendMessage(player, Settings.msgTradePermRemoved, "{perm}", currentTradePerm);
		} else if (newTradePerm != null) {
			// set trade permission:
			Utils.sendMessage(player, Settings.msgTradePermSet, "{perm}", newTradePerm);
		} else {
			// display current trade permission:
			Utils.sendMessage(player, Settings.msgTradePermView, "{perm}", currentTradePerm);
			return;
		}

		// set trade permission:
		((AdminShopkeeper) shopkeeper).setTradePermission(newTradePerm);

		// save:
		shopkeeper.save();
	}
}
