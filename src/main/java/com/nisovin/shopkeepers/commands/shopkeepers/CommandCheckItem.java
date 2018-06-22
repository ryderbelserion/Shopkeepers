package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.util.ItemUtils;

class CommandCheckItem extends PlayerCommand {

	CommandCheckItem() {
		super("checkitem");

		// set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// set description:
		this.setDescription("Shows debugging information for the currently held items.");

		// hidden debugging command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		ItemStack inHand = player.getItemInHand();
		int holdSlot = player.getInventory().getHeldItemSlot();
		ItemStack nextItem = player.getInventory().getItem(holdSlot == 8 ? 0 : holdSlot + 1);

		player.sendMessage("Item in hand:");
		player.sendMessage("-Is low currency: " + (Settings.isCurrencyItem(inHand)));
		player.sendMessage("-Is high currency: " + (Settings.isHighCurrencyItem(inHand)));
		player.sendMessage("-Is low zero currency: " + (Settings.isZeroCurrencyItem(inHand)));
		player.sendMessage("-Is high zero currency: " + (Settings.isHighZeroCurrencyItem(inHand)));
		player.sendMessage("-Similar to next item: " + (ItemUtils.isSimilar(nextItem, inHand) ? "yes" : "nope"));

		player.sendMessage("Next item:");
		player.sendMessage("-Is low currency: " + (Settings.isCurrencyItem(nextItem)));
		player.sendMessage("-Is high currency: " + (Settings.isHighCurrencyItem(nextItem)));
		player.sendMessage("-Is low zero currency: " + (Settings.isZeroCurrencyItem(nextItem)));
		player.sendMessage("-Is high zero currency: " + (Settings.isHighZeroCurrencyItem(nextItem)));
	}
}
