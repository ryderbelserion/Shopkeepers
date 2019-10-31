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
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyStringFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandList extends Command {

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
		this.addArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				// matches names of online players, but falls back to any given name or the sender's name
				// using a fallback instead of adjusting the filter, so that the following page argument has a chance to
				// parse the argument first before the fallback gets used
				new SenderPlayerNameFallback(new AnyStringFallback(new PlayerNameArgument(ARGUMENT_PLAYER, PlayerNameArgument.ACCEPT_ONLINE_PLAYERS)))),
				true, true));
		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_PAGE), 1));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		int page = context.get(ARGUMENT_PAGE);
		String targetPlayerName = context.get(ARGUMENT_PLAYER);
		if (context.has(ARGUMENT_ADMIN)) {
			// list admin shopkeepers:
			targetPlayerName = null;
		}

		List<Shopkeeper> shops = new ArrayList<>();

		if (targetPlayerName == null) {
			// permission check:
			this.checkPermission(sender, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);

			// searching admin shops:
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (!(shopkeeper instanceof PlayerShopkeeper)) {
					shops.add(shopkeeper);
				}
			}
		} else {
			// permission check:
			if (sender instanceof Player && targetPlayerName.equals(sender.getName())) {
				// list own player shopkeepers:
				this.checkPermission(sender, ShopkeepersPlugin.LIST_OWN_PERMISSION);
			} else {
				// list other player shopkeepers:
				this.checkPermission(sender, ShopkeepersPlugin.LIST_OTHERS_PERMISSION);
			}

			// searching shops of specific player:
			Player listPlayer = Bukkit.getPlayerExact(targetPlayerName);
			UUID listPlayerUUID = (listPlayer != null ? listPlayer.getUniqueId() : null);

			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
					if (playerShop.getOwnerName().equals(targetPlayerName)) {
						UUID shopOwnerUUID = playerShop.getOwnerUUID();
						if (shopOwnerUUID == null || shopOwnerUUID.equals(listPlayerUUID) || listPlayerUUID == null) {
							shops.add(playerShop);
						}
					}
				}
			}
		}

		int shopsCount = shops.size();
		int maxPage = Math.max(1, (int) Math.ceil((double) shopsCount / ENTRIES_PER_PAGE));
		page = Math.max(1, Math.min(page, maxPage));

		if (targetPlayerName == null) {
			// listing admin shops:
			TextUtils.sendMessage(sender, Settings.msgListAdminShopsHeader,
					"{shopsCount}", String.valueOf(shopsCount),
					"{page}", String.valueOf(page),
					"{maxPage}", String.valueOf(maxPage));
		} else {
			// listing player shops:
			TextUtils.sendMessage(sender, Settings.msgListPlayerShopsHeader,
					"{player}", targetPlayerName,
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
			TextUtils.sendMessage(sender, Settings.msgListShopsEntry,
					"{shopIndex}", String.valueOf(index + 1),
					"{shopUUID}", shopkeeper.getUniqueId().toString(),
					// deprecated, use {shopId} instead; TODO remove at some point
					"{shopSessionId}", String.valueOf(shopkeeper.getId()),
					"{shopId}", String.valueOf(shopkeeper.getId()),
					"{shopName}", (hasName ? (shopName + " ") : ""),
					"{location}", shopkeeper.getPositionString(),
					"{shopType}", shopkeeper.getType().getIdentifier(),
					"{objectType}", shopkeeper.getShopObject().getType().getIdentifier());
		}
	}
}
