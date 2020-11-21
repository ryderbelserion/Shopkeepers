package com.nisovin.shopkeepers.commands.shopkeepers;

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
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandGive extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_AMOUNT = "amount";

	CommandGive() {
		super("give");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.GIVE_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionGive);

		// Arguments:
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_AMOUNT), 1));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		assert targetPlayer != null;
		boolean targetSelf = (sender.equals(targetPlayer));

		int amount = context.get(ARGUMENT_AMOUNT);
		assert amount >= 1;
		// Upper limit to avoid accidental misuse: TODO Use BoundedIntegerArgument in the future?
		if (amount > 1024) amount = 1024;

		ItemStack item = Settings.createShopCreationItem();
		item.setAmount(amount);

		PlayerInventory inventory = targetPlayer.getInventory();
		ItemStack[] contents = inventory.getStorageContents();
		int remaining = ItemUtils.addItems(contents, item);
		ItemUtils.setStorageContents(inventory, contents);
		if (remaining > 0) {
			item.setAmount(remaining);
			targetPlayer.getWorld().dropItem(targetPlayer.getEyeLocation(), item);
		}

		// Success:
		// TODO Show shop creation item via hover text?
		// Inform target player:
		TextUtils.sendMessage(targetPlayer, Messages.shopCreationItemsReceived,
				"amount", amount
		);
		if (!targetSelf) {
			// Inform command executor:
			TextUtils.sendMessage(sender, Messages.shopCreationItemsGiven,
					"player", TextUtils.getPlayerText(targetPlayer),
					"amount", amount
			);
		}
	}
}
