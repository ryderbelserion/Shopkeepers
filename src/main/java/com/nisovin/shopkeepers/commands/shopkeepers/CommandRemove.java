package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandRemove extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	private final Confirmations confirmations;

	CommandRemove(Confirmations confirmations) {
		super("remove", Arrays.asList("delete"));
		this.confirmations = confirmations;

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionRemove);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER, true),
				TargetShopkeeperFilter.ANY
		));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		Player senderPlayer = (sender instanceof Player) ? (Player) sender : null;

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		assert shopkeeper != null;

		// Permission checks:
		if (shopkeeper instanceof PlayerShopkeeper) {
			PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
			if (senderPlayer != null && playerShop.isOwner(senderPlayer)) {
				this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION);
			} else {
				this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION);
			}
		} else {
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
		}

		confirmations.awaitConfirmation(sender, () -> {
			if (!shopkeeper.isValid()) {
				// The shopkeeper has been removed in the meantime.
				TextUtils.sendMessage(sender, Messages.shopAlreadyRemoved);
				return;
			}

			if (senderPlayer != null) {
				// Call event:
				PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(shopkeeper, senderPlayer);
				if (deleteEvent.isCancelled()) {
					TextUtils.sendMessage(sender, Messages.shopRemovalCancelled);
					return;
				}
			}

			// Delete and save:
			shopkeeper.delete(senderPlayer);
			shopkeeper.save();

			TextUtils.sendMessage(sender, Messages.shopRemoved);
		});

		TextUtils.sendMessage(sender, Messages.confirmRemoveShop);
		TextUtils.sendMessage(sender, Messages.confirmationRequired);
	}
}
