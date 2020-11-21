package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandGiveCurrency extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_CURRENCY_TYPE = "currency-type";
	private static final String ARGUMENT_CURRENCY_LOW = "low";
	private static final String ARGUMENT_CURRENCY_HIGH = "high";
	private static final String ARGUMENT_AMOUNT = "amount";

	CommandGiveCurrency() {
		super("givecurrency", Arrays.asList("currency"));

		// Set permission:
		this.setPermission(ShopkeepersPlugin.GIVE_CURRENCY_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionGiveCurrency);

		// Arguments:
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
		this.addArgument(new DefaultValueFallback<String>(new TypedFirstOfArgument<>(ARGUMENT_CURRENCY_TYPE, Arrays.asList(
				new LiteralArgument(ARGUMENT_CURRENCY_LOW),
				new LiteralArgument(ARGUMENT_CURRENCY_HIGH))),
				ARGUMENT_CURRENCY_LOW));
		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_AMOUNT), 1));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		assert targetPlayer != null;
		boolean targetSelf = (sender.equals(targetPlayer));

		String currencyType = context.get(ARGUMENT_CURRENCY_TYPE);
		boolean lowCurrency;
		if (ARGUMENT_CURRENCY_LOW.equals(currencyType)) {
			lowCurrency = true;
		} else {
			assert ARGUMENT_CURRENCY_HIGH.equals(currencyType);
			lowCurrency = false;
			if (!Settings.isHighCurrencyEnabled()) {
				TextUtils.sendMessage(sender, Messages.highCurrencyDisabled);
				return;
			}
		}

		int amount = context.get(ARGUMENT_AMOUNT);
		assert amount >= 1;
		// Upper limit to avoid accidental misuse: TODO Use BoundedIntegerArgument in the future?
		if (amount > 1024) amount = 1024;

		ItemStack item;
		if (lowCurrency) {
			item = Settings.createCurrencyItem(amount);
		} else {
			item = Settings.createHighCurrencyItem(amount);
		}
		assert item != null;

		PlayerInventory inventory = targetPlayer.getInventory();
		ItemStack[] contents = inventory.getStorageContents();
		int remaining = ItemUtils.addItems(contents, item);
		ItemUtils.setStorageContents(inventory, contents);
		if (remaining > 0) {
			item.setAmount(remaining);
			targetPlayer.getWorld().dropItem(targetPlayer.getEyeLocation(), item);
		}

		// Success:
		// TODO Show currency item via hover text?
		if (lowCurrency) {
			// Inform target player:
			TextUtils.sendMessage(targetPlayer, Messages.currencyItemsReceived,
					"amount", amount
			);
			if (!targetSelf) {
				// Inform command executor:
				TextUtils.sendMessage(sender, Messages.currencyItemsGiven,
						"player", TextUtils.getPlayerText(targetPlayer),
						"amount", amount
				);
			}
		} else {
			// Inform target player:
			TextUtils.sendMessage(targetPlayer, Messages.highCurrencyItemsReceived,
					"amount", amount
			);
			if (!targetSelf) {
				// Inform command executor:
				TextUtils.sendMessage(sender, Messages.highCurrencyItemsGiven,
						"player", TextUtils.getPlayerText(targetPlayer),
						"amount", amount
				);
			}
		}
	}
}
