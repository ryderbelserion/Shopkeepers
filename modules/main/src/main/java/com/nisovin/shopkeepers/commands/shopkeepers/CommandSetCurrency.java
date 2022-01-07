package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

class CommandSetCurrency extends PlayerCommand {

	private static final String ARGUMENT_CURRENCY_TYPE = "currency-type";
	private static final String ARGUMENT_CURRENCY_TYPE_LOW = "low";
	private static final String ARGUMENT_CURRENCY_TYPE_HIGH = "high";

	CommandSetCurrency() {
		super("setCurrency");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SET_CURRENCY_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSetCurrency);

		// Arguments:
		this.addArgument(new DefaultValueFallback<>(new TypedFirstOfArgument<>(ARGUMENT_CURRENCY_TYPE, Arrays.asList(
				new LiteralArgument(ARGUMENT_CURRENCY_TYPE_LOW),
				new LiteralArgument(ARGUMENT_CURRENCY_TYPE_HIGH))),
				ARGUMENT_CURRENCY_TYPE_LOW));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert input.getSender() instanceof Player;
		Player player = (Player) input.getSender();

		String currencyType = context.get(ARGUMENT_CURRENCY_TYPE);
		boolean lowCurrency;
		if (ARGUMENT_CURRENCY_TYPE_LOW.equals(currencyType)) {
			lowCurrency = true;
		} else {
			assert ARGUMENT_CURRENCY_TYPE_HIGH.equals(currencyType);
			lowCurrency = false;
		}

		ItemStack newCurrencyItem = ItemUtils.getOrEmpty(player.getInventory().getItemInMainHand());

		// Note: The high currency item can be set to AIR to disable the high currency.
		if (lowCurrency && ItemUtils.isEmpty(newCurrencyItem)) {
			TextUtils.sendMessage(player, Messages.mustHoldItemInMainHand);
			return;
		}

		if (lowCurrency) {
			Settings.currencyItem = new ItemData(newCurrencyItem);
			Settings.onSettingsChanged();
			Settings.saveConfig();
			TextUtils.sendMessage(player, Messages.currencyItemSetToMainHandItem);
		} else {
			Settings.highCurrencyItem = new ItemData(newCurrencyItem);
			Settings.onSettingsChanged();
			Settings.saveConfig();
			TextUtils.sendMessage(player, Messages.highCurrencyItemSetToMainHandItem);
		}
	}
}
