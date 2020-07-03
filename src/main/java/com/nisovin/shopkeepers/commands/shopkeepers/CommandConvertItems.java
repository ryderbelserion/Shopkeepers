package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandConvertItems extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_ALL = "all";

	CommandConvertItems() {
		super("convertItems", Arrays.asList("convertItem", "convert"));

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionConvertItems);

		// arguments:
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
		this.addArgument(new OptionalArgument<>(new LiteralArgument(ARGUMENT_ALL)));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OTHERS_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		assert targetPlayer != null;
		boolean targetSelf = (sender.equals(targetPlayer));

		boolean convertAll = context.has(ARGUMENT_ALL);

		// Check permission:
		if (targetSelf) {
			this.checkPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OWN_PERMISSION);
		} else {
			this.checkPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OTHERS_PERMISSION);
		}

		PlayerInventory inventory = targetPlayer.getInventory();
		int convertedStacks = 0;
		if (convertAll) {
			ItemStack[] contents = inventory.getContents(); // includes armor and off hand slot
			convertedStacks = ItemUtils.convertItems(contents, (item) -> true);
			if (convertedStacks > 0) {
				// Apply changes back to inventory:
				ItemUtils.setContents(inventory, contents);
			}
		} else {
			// Convert held item:
			ItemStack itemInHand = inventory.getItemInMainHand();
			if (!ItemUtils.isEmpty(itemInHand)) {
				ItemStack convertedItem = ItemUtils.convertItem(itemInHand);
				if (!itemInHand.isSimilar(convertedItem)) {
					convertedStacks = 1;
					inventory.setItemInMainHand(convertedItem);
				}
			}
		}

		if (convertedStacks > 0) {
			targetPlayer.updateInventory();
		}

		// Inform target player:
		TextUtils.sendMessage(targetPlayer, Settings.msgItemsConverted,
				"count", convertedStacks
		);
		if (!targetSelf) {
			// Inform command executor:
			TextUtils.sendMessage(sender, Settings.msgItemsConverted,
					"count", convertedStacks
			);
		}
	}
}
