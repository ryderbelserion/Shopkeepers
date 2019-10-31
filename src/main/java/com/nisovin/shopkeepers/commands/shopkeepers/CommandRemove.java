package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandRemove extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_ALL = "all";
	private static final String ARGUMENT_ADMIN = "admin";

	private final ShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;
	private final Confirmations confirmations;

	CommandRemove(ShopkeepersPlugin plugin, ShopkeeperRegistry shopkeeperRegistry, Confirmations confirmations) {
		super("remove", Arrays.asList("delete"));
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.confirmations = confirmations;

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemove);

		// arguments: TODO allow specifying a single shopkeeper?
		this.addArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				new LiteralArgument(ARGUMENT_ALL),
				// not matching names of online players to avoid accidental matches
				// allows any given name or falls back to sender TODO alias 'own'?
				new SenderPlayerNameFallback(new PlayerNameArgument(ARGUMENT_PLAYER, ArgumentFilter.acceptAny(), false))),
				true, true));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		String targetPlayerName = context.get(ARGUMENT_PLAYER); // can be null
		boolean targetOwnShops = (sender instanceof Player && sender.getName().equals(targetPlayerName));
		boolean all = context.has(ARGUMENT_ALL);
		boolean admin = context.has(ARGUMENT_ADMIN);
		assert all || admin || targetPlayerName != null;

		// permission checks:
		if (admin) {
			// remove all admin shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
		} else if (all) {
			// remove all player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PERMISSION);
		} else if (targetOwnShops) {
			// remove own player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION);
		} else {
			// remove other player's shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION);
		}

		// this is dangerous: let the sender first confirm this action
		confirmations.awaitConfirmation(sender, () -> {
			List<Shopkeeper> shops = new ArrayList<>();
			if (admin) {
				// searching all admin shops:
				for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
					if (!(shopkeeper instanceof PlayerShopkeeper)) {
						shops.add(shopkeeper);
					}
				}
			} else if (all) {
				// searching all player shops:
				for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
					if (shopkeeper instanceof PlayerShopkeeper) {
						shops.add(shopkeeper);
					}
				}
			} else {
				assert targetPlayerName != null;
				// searching shops of specific player:
				Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
				UUID targetPlayerUUID = (targetPlayer != null) ? targetPlayer.getUniqueId() : null;

				for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
					if (shopkeeper instanceof PlayerShopkeeper) {
						PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
						if (playerShop.getOwnerName().equals(targetPlayerName)) {
							UUID shopOwnerUUID = playerShop.getOwnerUUID();
							// TODO really ignore owner uuid if the player is currently offline? - consider:
							// TODO * player A 'peter' creating shops
							// TODO * player A leaves, changes name, player B changes name to 'peter'
							// TODO * player B joins before player A has joined again yet, and creates shops
							// TODO * situation: shops with the same owner name, but different uuid.
							// Problem?
							// instead: output an error if the name is ambiguous and allow clarifying by specifying an
							// uuid instead of player name
							if (shopOwnerUUID == null || targetPlayerUUID == null || shopOwnerUUID.equals(targetPlayerUUID)) {
								shops.add(playerShop);
							}
						}
					}
				}
			}

			// removing shops:
			for (Shopkeeper shopkeeper : shops) {
				shopkeeper.delete();
			}

			// trigger save:
			plugin.getShopkeeperStorage().save();

			// printing result message:
			int shopsCount = shops.size();
			if (admin) {
				// removed all admin shops:
				TextUtils.sendMessage(sender, Settings.msgRemovedAdminShops,
						"{shopsCount}", String.valueOf(shopsCount));
			} else if (all) {
				// removed all player shops:
				TextUtils.sendMessage(sender, Settings.msgRemovedAllPlayerShops,
						"{shopsCount}", String.valueOf(shopsCount));
			} else {
				// removed shops of specific player:
				TextUtils.sendMessage(sender, Settings.msgRemovedPlayerShops,
						"{player}", targetPlayerName,
						"{shopsCount}", String.valueOf(shopsCount));
			}
		});

		// inform player about required confirmation:
		if (admin) {
			// removing all admin shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAdminShops);
		} else if (all) {
			// removing all player shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAllPlayerShops);
		} else if (targetOwnShops) {
			// removing own shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveOwnShops);
		} else {
			// removing shops of specific player:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemovePlayerShops,
					"{player}", targetPlayerName);
		}
		// inform player on how to confirm the action:
		TextUtils.sendMessage(sender, Settings.msgConfirmationRequired);
	}
}
