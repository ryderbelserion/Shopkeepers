package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.BoundedIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

class CommandGiveCurrency extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_CURRENCY = "currency";
	private static final String ARGUMENT_AMOUNT = "amount";

	CommandGiveCurrency() {
		super("giveCurrency", Arrays.asList("currency"));

		// Set permission:
		this.setPermission(ShopkeepersPlugin.GIVE_CURRENCY_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionGiveCurrency);

		// Arguments:
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
		// TODO Turn this into a proper argument with completions.
		this.addArgument(new StringArgument(ARGUMENT_CURRENCY).optional());
		// Upper limit to avoid accidental misuse:
		this.addArgument(new BoundedIntegerArgument(ARGUMENT_AMOUNT, 1, 1024).orDefaultValue(1));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		boolean targetSelf = (sender.equals(targetPlayer));

		Currency currency;
		String currencyType = context.getOrNull(ARGUMENT_CURRENCY);
		if (currencyType != null) {
			currency = Currencies.getById(StringUtils.normalize(currencyType));
			if (currency == null) {
				TextUtils.sendMessage(sender, Messages.unknownCurrency, "currency", currencyType);
				return;
			}
		} else {
			currency = Currencies.getBase();
		}
		assert currency != null;

		int amount = context.get(ARGUMENT_AMOUNT);
		assert amount >= 1 && amount <= 1024;

		ItemStack item = currency.getItemData().createItemStack(amount);
		assert item != null;

		PlayerInventory inventory = targetPlayer.getInventory();
		@Nullable ItemStack[] contents = Unsafe.castNonNull(inventory.getStorageContents());
		int remaining = InventoryUtils.addItems(contents, item);
		InventoryUtils.setStorageContents(inventory, contents);
		if (remaining > 0) {
			item.setAmount(remaining);
			targetPlayer.getWorld().dropItem(targetPlayer.getEyeLocation(), item);
		}

		// Success:
		// TODO Show currency item via hover text?
		// Inform target player:
		TextUtils.sendMessage(targetPlayer, Messages.currencyItemsReceived,
				"amount", amount,
				"currency", currency.getDisplayName(),
				"currencyId", currency.getId()
		);
		if (!targetSelf) {
			// Inform command executor:
			TextUtils.sendMessage(sender, Messages.currencyItemsGiven,
					"player", TextUtils.getPlayerText(targetPlayer),
					"amount", amount,
					"currency", currency.getDisplayName(),
					"currencyId", currency.getId()
			);
		}
	}
}
