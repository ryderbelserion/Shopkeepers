package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

class CommandTransfer extends PlayerCommand {

	private static final String ARGUMENT_NEW_OWNER = "new-owner";

	CommandTransfer() {
		super(Arrays.asList("transfer"));

		// set permission:
		this.setPermission(ShopkeepersPlugin.TRANSFER_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionTransfer);

		// arguments:
		this.addArgument(new PlayerArgument(ARGUMENT_NEW_OWNER));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Player newOwner = context.get(ARGUMENT_NEW_OWNER);
		assert newOwner != null;

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

		List<PlayerShopkeeper> shopkeepers = SKShopkeepersPlugin.getInstance().getProtectedChests().getShopkeeperOwnersOfChest(targetBlock);
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

		// set new owner:
		for (PlayerShopkeeper shopkeeper : shopkeepers) {
			shopkeeper.setOwner(newOwner);
		}
		Utils.sendMessage(player, Settings.msgOwnerSet, "{owner}", newOwner.getName());

		// save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
