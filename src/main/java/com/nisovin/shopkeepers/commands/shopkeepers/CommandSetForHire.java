package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.util.ItemUtils;
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

		// get targeted block:
		Block targetBlock = null;
		try {
			targetBlock = player.getTargetBlock((Set<Material>) null, 10);
		} catch (Exception e) {
			// getTargetBlock might sometimes throw an exception
		}

		if (targetBlock == null || !ItemUtils.isChest(targetBlock.getType())) {
			Utils.sendMessage(player, Settings.msgMustTargetChest);
			return;
		}

		List<PlayerShopkeeper> shopkeepers = SKShopkeepersPlugin.getInstance().getProtectedChests().getShopkeepersUsingChest(targetBlock);
		if (shopkeepers.size() == 0) {
			Utils.sendMessage(player, Settings.msgUnusedChest);
			return;
		}

		if (!Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			for (PlayerShopkeeper shopkeeper : shopkeepers) {
				if (!shopkeeper.isOwner(player)) {
					Utils.sendMessage(player, Settings.msgNotOwner);
					return;
				}
			}
		}

		ItemStack hireCost = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(hireCost)) {
			Utils.sendMessage(player, Settings.msgMustHoldHireItem);
			return;
		}

		for (PlayerShopkeeper shopkeeper : shopkeepers) {
			shopkeeper.setForHire(hireCost);
		}
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
		Utils.sendMessage(player, Settings.msgSetForHire);
	}
}
