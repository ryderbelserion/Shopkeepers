package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ItemUtils;

class CommandCheckItem extends PlayerCommand {

	CommandCheckItem() {
		super("checkitem");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Shows debugging information for the currently held items."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		ItemStack mainHandItem = player.getInventory().getItemInMainHand();
		ItemStack offHandItem = player.getInventory().getItemInOffHand();

		player.sendMessage(ChatColor.YELLOW + "Item in main hand:");
		player.sendMessage("- Is low currency: " + (Settings.isCurrencyItem(mainHandItem)));
		player.sendMessage("- Is high currency: " + (Settings.isHighCurrencyItem(mainHandItem)));
		player.sendMessage("- Is zero low currency: " + (Settings.isZeroCurrencyItem(mainHandItem)));
		player.sendMessage("- Is zero high currency: " + (Settings.isZeroHighCurrencyItem(mainHandItem)));
		player.sendMessage("- Similar to off-hand item: " + (ItemUtils.isSimilar(mainHandItem, offHandItem) ? "yes" : "nope"));
		player.sendMessage("- Matching off-hand item: " + (ItemUtils.matchesData(mainHandItem, offHandItem) ? "yes" : "nope"));

		player.sendMessage(ChatColor.YELLOW + "Item in off-hand:");
		player.sendMessage("- Is low currency: " + (Settings.isCurrencyItem(offHandItem)));
		player.sendMessage("- Is high currency: " + (Settings.isHighCurrencyItem(offHandItem)));
		player.sendMessage("- Is zero low currency: " + (Settings.isZeroCurrencyItem(offHandItem)));
		player.sendMessage("- Is zero high currency: " + (Settings.isZeroHighCurrencyItem(offHandItem)));
	}
}
