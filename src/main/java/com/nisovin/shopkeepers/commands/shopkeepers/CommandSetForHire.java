package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandSetForHire extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandSetForHire() {
		super("setForHire");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSetforhire);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, ShopkeeperFilter.PLAYER),
				TargetShopkeeperFilter.PLAYER)
		);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		PlayerShopkeeper shopkeeper = (PlayerShopkeeper) context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;

		ItemStack hireCost = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(hireCost)) {
			// TODO Allow disabling hiring again, maybe with empty hand?
			// TODO Show hire item via hover event?
			TextUtils.sendMessage(player, Messages.mustHoldHireItem);
			return;
		}

		// Check that the shop is owned by the executing player:
		if (!shopkeeper.isOwner(player) && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			TextUtils.sendMessage(player, Messages.notOwner);
			return;
		} else {
			// Set for hire:
			shopkeeper.setForHire(hireCost);
		}

		// Success:
		TextUtils.sendMessage(player, Messages.setForHire);

		// Save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
