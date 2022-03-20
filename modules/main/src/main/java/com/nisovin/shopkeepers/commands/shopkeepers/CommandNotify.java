package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.tradenotifications.NotificationUserPreferences;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandNotify extends PlayerCommand {

	private static final String ARGUMENT_TRADES = "trades";

	CommandNotify() {
		super("notify");

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionNotify);

		// Arguments:
		this.addArgument(new FirstOfArgument("notification-type", Arrays.asList(
				new LiteralArgument(ARGUMENT_TRADES)
		))); // Join formats, but don't reverse
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.NOTIFY_TRADES_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		Player sender = (Player) input.getSender();
		// This is the only kind of notification that can be toggled currently:
		assert context.has(ARGUMENT_TRADES);
		if (context.has(ARGUMENT_TRADES)) {
			NotificationUserPreferences userPreferences = SKShopkeepersPlugin.getInstance().getTradeNotifications().getUserPreferences();
			boolean newState = !userPreferences.isNotifyOnTrades(sender);
			userPreferences.setNotifyOnTrades(sender, newState);

			// Feedback message:
			if (newState) {
				TextUtils.sendMessage(sender, Messages.tradeNotificationsEnabled);
			} else {
				TextUtils.sendMessage(sender, Messages.tradeNotificationsDisabled);
			}
		}
	}
}
