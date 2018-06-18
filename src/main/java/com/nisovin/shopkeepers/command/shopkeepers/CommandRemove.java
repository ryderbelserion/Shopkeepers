package com.nisovin.shopkeepers.command.shopkeepers;

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
import com.nisovin.shopkeepers.command.Confirmations;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandException;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.command.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.command.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.command.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.command.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.util.Utils;

class CommandRemove extends PlayerCommand {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_ALL = "all";
	private static final String ARGUMENT_ADMIN = "admin";

	private final ShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;
	private final Confirmations confirmations;

	CommandRemove(ShopkeepersPlugin plugin, ShopkeeperRegistry shopkeeperRegistry, Confirmations confirmations) {
		super(Arrays.asList("remove", "delete"));
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.confirmations = confirmations;

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionRemove);

		// arguments:
		this.addArgument(new OptionalArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				new LiteralArgument(ARGUMENT_ALL),
				new StringArgument(ARGUMENT_PLAYER)), true, true)));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return Utils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		String playerName = context.getOrDefault(ARGUMENT_PLAYER, player.getName());
		boolean all = context.has(ARGUMENT_ALL);
		boolean admin = context.has(ARGUMENT_ADMIN);

		// permission checks:
		if (admin) {
			// remove admin shopkeepers:
			this.checkPermission(player, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
		} else if (all) {
			// remove all player shopkeepers:
			this.checkPermission(player, ShopkeepersPlugin.REMOVE_ALL_PERMISSION);
		} else if (playerName.equals(player.getName())) {
			// remove own player shopkeepers:
			this.checkPermission(player, ShopkeepersPlugin.REMOVE_OWN_PERMISSION);
		} else {
			// remove other player shopkeepers:
			this.checkPermission(player, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION);
		}

		// this is dangerous: let the player first confirm this action
		confirmations.awaitConfirmation(player, () -> {
			List<Shopkeeper> shops = new ArrayList<>();
			if (admin) {
				// searching admin shops:
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
				// searching shops of specific player:
				Player listPlayer = Bukkit.getPlayerExact(playerName);
				UUID listPlayerUUID = (listPlayer != null ? listPlayer.getUniqueId() : null);

				for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
					if (shopkeeper instanceof PlayerShopkeeper) {
						PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
						if (playerShop.getOwnerName().equals(playerName)) {
							UUID shopOwnerUUID = playerShop.getOwnerUUID();
							// TODO really ignore owner uuid if the player is currently offline? - consider:
							// TODO * player A 'peter' creating shops
							// TODO * player A leaves, changes name, player B changes name to 'peter'
							// TODO * player B joins before player A has joined again yet, and creates shops
							// TODO * situation: shops with the same owner name, but different uuid.
							// Problem?
							// instead: allow specifying an uuid instead of player name and then detect if there are
							// shops with the same owner name but different uuids
							if (shopOwnerUUID == null || listPlayerUUID == null || shopOwnerUUID.equals(listPlayerUUID)) {
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
				// removed admin shops:
				Utils.sendMessage(player, Settings.msgRemovedAdminShops,
						"{shopsCount}", String.valueOf(shopsCount));
			} else if (all) {
				// removed all player shops:
				Utils.sendMessage(player, Settings.msgRemovedAllPlayerShops,
						"{shopsCount}", String.valueOf(shopsCount));
			} else {
				// removed shops of specific player:
				Utils.sendMessage(player, Settings.msgRemovedPlayerShops,
						"{player}", playerName,
						"{shopsCount}", String.valueOf(shopsCount));
			}
		});

		// inform player about required confirmation:
		if (admin) {
			// removing admin shops:
			Utils.sendMessage(player, Settings.msgConfirmRemoveAdminShops);
		} else if (all) {
			// removing all player shops:
			Utils.sendMessage(player, Settings.msgConfirmRemoveAllPlayerShops);
		} else if (playerName.equals(player.getName())) {
			// removing own shops:
			Utils.sendMessage(player, Settings.msgConfirmRemoveOwnShops);
		} else {
			// removing shops of specific player:
			Utils.sendMessage(player, Settings.msgConfirmRemovePlayerShops,
					"{player}", playerName);
		}
		// inform player on how to confirm the action:
		Utils.sendMessage(player, Settings.msgConfirmationRequired);
	}
}
