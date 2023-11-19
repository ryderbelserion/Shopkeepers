package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.trading.commandtrading.CommandTradingUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

class CommandSetTradedCommand extends PlayerCommand {

	private static final String ARGUMENT_NEW_COMMAND = "command";
	private static final String ARGUMENT_REMOVE_COMMAND = "-";
	private static final String ARGUMENT_QUERY_COMMAND = "?";

	CommandSetTradedCommand() {
		super("setTradedCommand");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SET_TRADED_COMMAND_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSettradedcommand);

		// Arguments:
		this.addArgument(new FirstOfArgument("commandArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY_COMMAND)
						.orDefaultValue(ARGUMENT_QUERY_COMMAND),
				new LiteralArgument(ARGUMENT_REMOVE_COMMAND),
				new StringArgument(ARGUMENT_NEW_COMMAND, true)
		), true, true));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(itemInHand)) {
			TextUtils.sendMessage(player, Messages.mustHoldItemInMainHand);
			return;
		}

		String newTradedCommand = context.getOrNull(ARGUMENT_NEW_COMMAND);
		boolean removeCommand = context.has(ARGUMENT_REMOVE_COMMAND);

		if (removeCommand) {
			// Remove the traded command:
			String tradedCommand = getTradedCommandView(itemInHand);
			CommandTradingUtils.setTradedCommand(itemInHand, null);
			TextUtils.sendMessage(player, Messages.tradedCommandRemoved, "command", tradedCommand);
			return;
		} else if (newTradedCommand != null) {
			// Set the traded command:
			CommandTradingUtils.setTradedCommand(itemInHand, newTradedCommand);
			TextUtils.sendMessage(player, Messages.tradedCommandSet, "command", newTradedCommand);
			return;
		} else {
			// Display the currently traded command:
			String tradedCommand = CommandTradingUtils.getTradedCommand(itemInHand);
			if (tradedCommand == null) {
				TextUtils.sendMessage(player, Messages.tradedCommandViewUnset);
			} else {
				TextUtils.sendMessage(player, Messages.tradedCommandView, "command", tradedCommand);
			}
			return;
		}
	}

	private static String getTradedCommandView(ItemStack itemStack) {
		String tradedCommand = CommandTradingUtils.getTradedCommand(itemStack);
		if (tradedCommand == null) tradedCommand = "-";
		return tradedCommand;
	}
}
