package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.shopkeeper.ShopTypeCategory;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;
import com.nisovin.shopkeepers.util.Utils;

class CommandSetForHire extends PlayerCommand {

	CommandSetForHire() {
		super("setForHire");

		// set permission:
		this.setPermission(ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionSetforhire);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		List<? extends Shopkeeper> shopkeepers = ShopkeeperUtils.getTargetedShopkeepers(player, ShopTypeCategory.PLAYER, true);
		if (shopkeepers.isEmpty()) return; // messages were already handled

		ItemStack hireCost = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(hireCost)) {
			// TODO allow disabling hiring again, with empty hand
			Utils.sendMessage(player, Settings.msgMustHoldHireItem);
			return;
		}

		// set for hire:
		final boolean bypass = Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION);
		int affectedShops = 0;
		for (Shopkeeper shopkeeper : shopkeepers) {
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper; // this got already checked
			// only transfer shops that are owned by the player:
			if (bypass || playerShopkeeper.isOwner(player)) {
				playerShopkeeper.setForHire(hireCost);
				affectedShops++;
			}
		}

		// inform if there was no single shopkeeper that could be set for hire:
		assert !shopkeepers.isEmpty();
		if (affectedShops == 0) {
			Utils.sendMessage(player, Settings.msgNotOwner);
			return;
		}

		// success:
		Utils.sendMessage(player, Settings.msgSetForHire);

		// save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
