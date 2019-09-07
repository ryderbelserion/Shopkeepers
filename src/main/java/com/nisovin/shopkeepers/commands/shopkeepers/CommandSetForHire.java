package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.arguments.PlayerShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandSetForHire extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandSetForHire() {
		super("setForHire");

		// set permission:
		this.setPermission(ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionSetforhire);

		// arguments:
		this.addArgument(new TargetShopkeeperFallback(new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, PlayerShopkeeperFilter.INSTANCE), TargetShopkeeperFilter.PLAYER));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		PlayerShopkeeper shopkeeper = (PlayerShopkeeper) context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;

		ItemStack hireCost = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(hireCost)) {
			// TODO allow disabling hiring again, with empty hand
			TextUtils.sendMessage(player, Settings.msgMustHoldHireItem);
			return;
		}

		// check that the shop is owned by the executing player:
		if (!shopkeeper.isOwner(player) && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			TextUtils.sendMessage(player, Settings.msgNotOwner);
			return;
		} else {
			// set for hire:
			shopkeeper.setForHire(hireCost);
		}

		// success:
		TextUtils.sendMessage(player, Settings.msgSetForHire);

		// save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
