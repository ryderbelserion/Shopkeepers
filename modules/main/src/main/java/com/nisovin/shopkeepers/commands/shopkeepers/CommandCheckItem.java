package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

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

		player.sendMessage(ChatColor.YELLOW + "Item in main hand / off hand:");
		player.sendMessage("- Similar to off-hand item: "
				+ toDisplayString(ItemUtils.isSimilar(mainHandItem, offHandItem)));
		player.sendMessage("- Matching off-hand item: "
				+ toDisplayString(ItemUtils.matchesData(mainHandItem, offHandItem)));
		player.sendMessage("- MC matching off-hand item: "
				+ toDisplayString(NMSManager.getProvider().matches(mainHandItem, offHandItem)));
		player.sendMessage("- Is shop creation item: "
				+ checkItems(mainHandItem, offHandItem, ShopCreationItem::isShopCreationItem));
		for (Currency currency : Currencies.getAll()) {
			player.sendMessage("- Is currency item '" + currency.getId() + "': "
					+ checkItems(mainHandItem, offHandItem, currency.getItemData()::matches));
		}
	}

	private static String checkItems(
			ItemStack mainHand,
			ItemStack offHand,
			Predicate<? super @NonNull ItemStack> check
	) {
		return toDisplayString(check.test(mainHand)) + " / " + toDisplayString(check.test(offHand));
	}

	private static String toDisplayString(boolean bool) {
		return bool ? "yes" : "nope";
	}
}
