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
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.IntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.util.Utils;

class CommandList extends PlayerCommand {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_ADMIN = "admin";
	private static final String ARGUMENT_PAGE = "page";

	private static final int ENTRIES_PER_PAGE = 8;

	private final ShopkeeperRegistry shopkeeperRegistry;

	CommandList(ShopkeeperRegistry shopkeeperRegistry) {
		super("list");
		this.shopkeeperRegistry = shopkeeperRegistry;

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionList);

		// arguments:
		this.addArgument(new OptionalArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				new StringArgument(ARGUMENT_PLAYER)), true, true)));
		this.addArgument(new OptionalArgument(new IntegerArgument(ARGUMENT_PAGE)));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return Utils.hasPermission(sender, ShopkeepersPlugin.LIST_OWN_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersPlugin.LIST_OTHERS_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();
		int page = context.getOrDefault(ARGUMENT_PAGE, 1);
		// list own shops:
		String playerName = context.getOrDefault(ARGUMENT_PLAYER, player.getName());
		if (context.has(ARGUMENT_ADMIN)) {
			// list admin shopkeepers:
			playerName = null;
		}

		List<Shopkeeper> shops = new ArrayList<>();

		if (playerName == null) {
			// permission check:
			this.checkPermission(player, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);

			// searching admin shops:
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (!(shopkeeper instanceof PlayerShopkeeper)) {
					shops.add(shopkeeper);
				}
			}
		} else {
			// permission check:
			if (playerName.equals(player.getName())) {
				// list own player shopkeepers:
				this.checkPermission(player, ShopkeepersPlugin.LIST_OWN_PERMISSION);
			} else {
				// list other player shopkeepers:
				this.checkPermission(player, ShopkeepersPlugin.LIST_OTHERS_PERMISSION);
			}

			// searching shops of specific player:
			Player listPlayer = Bukkit.getPlayerExact(playerName);
			UUID listPlayerUUID = (listPlayer != null ? listPlayer.getUniqueId() : null);

			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
					if (playerShop.getOwnerName().equals(playerName)) {
						UUID shopOwnerUUID = playerShop.getOwnerUUID();
						if (shopOwnerUUID == null || shopOwnerUUID.equals(listPlayerUUID) || listPlayerUUID == null) {
							shops.add(playerShop);
						}
					}
				}
			}
		}

		int shopsCount = shops.size();
		int maxPage = (int) Math.ceil((double) shopsCount / ENTRIES_PER_PAGE);
		page = Math.max(1, Math.min(page, maxPage));

		if (playerName == null) {
			// listing admin shops:
			Utils.sendMessage(player, Settings.msgListAdminShopsHeader,
					"{shopsCount}", String.valueOf(shopsCount),
					"{page}", String.valueOf(page),
					"{maxPage}", String.valueOf(maxPage));
		} else {
			// listing player shops:
			Utils.sendMessage(player, Settings.msgListPlayerShopsHeader,
					"{player}", playerName,
					"{shopsCount}", String.valueOf(shopsCount),
					"{page}", String.valueOf(page),
					"{maxPage}", String.valueOf(maxPage));
		}

		int startIndex = (page - 1) * ENTRIES_PER_PAGE;
		int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, shopsCount);
		for (int index = startIndex; index < endIndex; index++) {
			Shopkeeper shopkeeper = shops.get(index);
			String shopName = shopkeeper.getName();
			boolean hasName = shopName != null && !shopName.isEmpty();
			Utils.sendMessage(player, Settings.msgListShopsEntry,
					"{shopIndex}", String.valueOf(index + 1),
					"{shopId}", shopkeeper.getUniqueId().toString(),
					"{shopSessionId}", String.valueOf(shopkeeper.getId()),
					"{shopName}", (hasName ? (shopName + " ") : ""),
					"{location}", shopkeeper.getPositionString(),
					"{shopType}", shopkeeper.getType().getIdentifier(),
					"{objectType}", shopkeeper.getShopObject().getObjectType().getIdentifier());
		}
	}
}
