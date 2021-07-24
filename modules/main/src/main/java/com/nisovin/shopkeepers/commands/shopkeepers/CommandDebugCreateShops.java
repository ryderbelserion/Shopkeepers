package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.text.Text;

class CommandDebugCreateShops extends PlayerCommand {

	private final static String ARGUMENT_SHOP_COUNT = "shopCount";

	private final SKShopkeepersPlugin plugin;

	CommandDebugCreateShops(SKShopkeepersPlugin plugin) {
		super("debugCreateShops");
		this.plugin = plugin;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Creates lots of shopkeepers for stress testing."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);

		// Arguments:
		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_SHOP_COUNT), 10));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();
		int shopCount = context.get(ARGUMENT_SHOP_COUNT);
		// Not using BoundedIntegerArgument for now due to missing descriptive error messages. TODO Use in the future.
		if (shopCount > 1000) {
			player.sendMessage(ChatColor.RED + "Shopkeeper count to high, limiting to 1000!");
			shopCount = 1000;
		}

		player.sendMessage(ChatColor.GREEN + "Creating up to " + shopCount + " shopkeepers, starting here!");
		int created = 0;
		Location curSpawnLocation = player.getLocation();
		for (int i = 0; i < shopCount; i++) {
			Shopkeeper shopkeeper = plugin.handleShopkeeperCreation(AdminShopCreationData.create(player, DefaultShopTypes.ADMIN_REGULAR(),
					DefaultShopObjectTypes.LIVING().get(EntityType.VILLAGER), curSpawnLocation.clone(), null));
			curSpawnLocation.add(2, 0, 0);
			if (shopkeeper != null) {
				created++;
			}
		}
		player.sendMessage(ChatColor.GREEN + "Done! Created " + ChatColor.YELLOW + created + ChatColor.GREEN + " shopkeepers!");
	}
}
