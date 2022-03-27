package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

class CommandSetCurrency extends PlayerCommand {

	private static final String ARGUMENT_CURRENCY = "currency";

	CommandSetCurrency() {
		super("setCurrency");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SET_CURRENCY_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSetCurrency);

		// Arguments:
		// TODO Turn this into a proper argument with completions.
		this.addArgument(new StringArgument(ARGUMENT_CURRENCY).optional());
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert input.getSender() instanceof Player;
		Player player = (Player) input.getSender();

		Currency currency;
		String currencyType = context.getOrNull(ARGUMENT_CURRENCY);
		if (currencyType != null) {
			currency = Currencies.getById(StringUtils.normalize(currencyType));
			if (currency == null) {
				TextUtils.sendMessage(player, Messages.unknownCurrency, "currency", currencyType);
				return;
			}
		} else {
			currency = Currencies.getBase();
		}
		assert currency != null;
		boolean baseCurrency = (currency == Currencies.getBase());

		ItemStack newCurrencyItem = ItemUtils.getOrEmpty(player.getInventory().getItemInMainHand());

		// Note: The high currency item can be set to AIR to disable the high currency.
		if (baseCurrency && ItemUtils.isEmpty(newCurrencyItem)) {
			TextUtils.sendMessage(player, Messages.mustHoldItemInMainHand);
			return;
		}

		// Close all open player shopkeeper UIs to ensure that the new currency item is immediately
		// in effect everywhere.
		// Note: This will not save any currently edited trades of open editors.
		// TODO Inform players why their UI sessions have been closed.
		// TODO Limit this to player shopkeepers that are actually affected by the currency item
		// change. Maybe let the shopkeepers themselves react to the settings change.
		new ArrayList<>(ShopkeepersAPI.getUIRegistry().getUISessions()).forEach(uiSession -> {
			if (uiSession.getShopkeeper() instanceof PlayerShopkeeper) {
				uiSession.abort();
			}
		});

		if (baseCurrency) {
			Settings.currencyItem = new ItemData(newCurrencyItem);
		} else {
			Settings.highCurrencyItem = new ItemData(newCurrencyItem);
		}

		Settings.onSettingsChanged();
		Settings.saveConfig();

		TextUtils.sendMessage(player, Messages.currencyItemSetToMainHandItem,
				"currency", currency.getDisplayName(),
				"currencyId", currency.getId());
	}
}
