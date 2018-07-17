package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.arguments.ShopObjectTypeArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopTypeArgument;
import com.nisovin.shopkeepers.commands.lib.BaseCommand;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.CommandRegistry;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.shopcreation.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopkeepersCommand extends BaseCommand {

	private static final String ARGUMENT_SHOP_TYPE = "shop-type";
	private static final String ARGUMENT_OBJECT_TYPE = "object-type";

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	public ShopkeepersCommand(SKShopkeepersPlugin plugin, PluginCommand pluginCommand, Confirmations confirmations) {
		super(pluginCommand);
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();

		// permission gets checked by testPermission and during execution

		// description:
		this.setDescription(Settings.msgCommandDescriptionShopkeeper);

		// formatting:
		this.setHelpTitleFormat(Utils.replaceArgs(Settings.msgCommandHelpTitle, "{version}", plugin.getDescription().getVersion()));
		this.setHelpUsageFormat(Settings.msgCommandHelpUsageFormat);
		this.setHelpDescFormat(Settings.msgCommandHelpDescriptionFormat);

		// arguments for shopkeeper creation:
		this.addArgument(new OptionalArgument(new ShopTypeArgument(ARGUMENT_SHOP_TYPE)));
		this.addArgument(new OptionalArgument(new ShopObjectTypeArgument(ARGUMENT_OBJECT_TYPE)));

		// register child commands:
		CommandRegistry childCommands = this.getChildCommands();
		childCommands.register(new CommandHelp(this));
		childCommands.register(new CommandReload(plugin));
		childCommands.register(new CommandDebug());
		childCommands.register(new CommandList(shopkeeperRegistry));
		childCommands.register(new CommandRemove(plugin, shopkeeperRegistry, confirmations));
		childCommands.register(new CommandRemote());
		childCommands.register(new CommandTransfer());
		childCommands.register(new CommandSetTradePerm());
		childCommands.register(new CommandSetForHire());
		// hidden commands:
		childCommands.register(new CommandConfirm(confirmations));
		// hidden debugging commands:
		childCommands.register(new CommandCheck(plugin));
		childCommands.register(new CommandCheckItem());
		childCommands.register(new CommandDebugCreateShops(plugin));
	}

	// also responsible for hiding the command from the help page, if shop creation via command is disabled
	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return Settings.createPlayerShopWithCommand || Utils.hasPermission(sender, ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		CommandSender sender = input.getSender();
		if (!(input.getSender() instanceof Player)) {
			throw PlayerCommand.createCommandSourceRejectedException(sender);
		}
		Player player = (Player) sender;

		// creating new shopkeeper:

		// get targeted block:
		Block targetBlock = null;
		try {
			targetBlock = player.getTargetBlock((Set<Material>) null, 10);
		} catch (Exception e) {
			// getTargetBlock might sometimes throw an exception
		}

		// check for valid targeted block:
		if (targetBlock == null || targetBlock.getType() == Material.AIR) {
			Utils.sendMessage(player, Settings.msgShopCreateFail);
			return;
		}

		ShopType<?> shopType = context.get(ARGUMENT_SHOP_TYPE);
		ShopObjectType<?> shopObjType = context.get(ARGUMENT_OBJECT_TYPE);

		boolean createPlayerShop = (Settings.createPlayerShopWithCommand && ItemUtils.isChest(targetBlock.getType()));
		if (createPlayerShop) {
			// create player shopkeeper:

			// check if this chest is already used by some other shopkeeper:
			if (plugin.getProtectedChests().isChestProtected(targetBlock, null)) {
				Utils.sendMessage(player, Settings.msgShopCreateFail);
				return;
			}

			// check for recently placed:
			if (Settings.requireChestRecentlyPlaced) {
				if (!plugin.getShopkeeperCreation().isRecentlyPlacedChest(player, targetBlock)) {
					Utils.sendMessage(player, Settings.msgChestNotPlaced);
					return;
				}
			}

			// check for permission:
			if (Settings.simulateRightClickOnCommand) {
				// simulating right click on the chest to check if access is denied:
				// making sure that access is really denied, and that the event is not cancelled because of denying
				// usage with the items in hands:
				PlayerInventory playerInventory = player.getInventory();
				ItemStack itemInMainHand = playerInventory.getItemInMainHand();
				ItemStack itemInOffHand = playerInventory.getItemInOffHand();
				playerInventory.setItemInMainHand(null);
				playerInventory.setItemInOffHand(null);

				TestPlayerInteractEvent fakeInteractEvent = new TestPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, targetBlock, BlockFace.UP);
				Bukkit.getPluginManager().callEvent(fakeInteractEvent);
				boolean chestAccessDenied = (fakeInteractEvent.useInteractedBlock() == Result.DENY);

				// resetting items in main and off hand:
				playerInventory.setItemInMainHand(itemInMainHand);
				playerInventory.setItemInOffHand(itemInOffHand);

				if (chestAccessDenied) {
					return;
				}
			}

			// default shop type and shop object type: first use-able player shop type and shop object type
			if (shopType == null) {
				shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
			}
			if (shopObjType == null) {
				shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);
			}

			if (shopType == null || shopObjType == null) {
				// TODO maybe print different kind of no-permission message,
				// because the player cannot create any shops at all:
				Utils.sendMessage(player, Settings.msgNoPermission);
				return;
			}
		} else {
			// create admin shopkeeper:

			// check permission:
			if (!Utils.hasPermission(player, ShopkeepersPlugin.ADMIN_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return;
			}

			// default shop type and shop object type:
			if (shopType == null) {
				shopType = DefaultShopTypes.ADMIN();
			}
			if (shopObjType == null) {
				shopObjType = plugin.getDefaultShopObjectType();
			}
			assert shopType != null && shopObjType != null;

			// can the selected shop type be used?
			// TODO considering the addition of arbitrary new shop types: how to identify the shop types that are
			// supported here?
			if (shopType instanceof PlayerShopType) {
				// only admin shop types are allowed here:
				// TODO message translation?
				Utils.sendMessage(player, ChatColor.RED + "You have to select an admin shop type!");
				return;
			}
		}
		assert shopType != null && shopObjType != null;

		// can the selected shop type be used?
		if (!shopType.hasPermission(player)) {
			Utils.sendMessage(player, Settings.msgNoPermission);
			return;
		}
		if (!shopType.isEnabled()) {
			Utils.sendMessage(player, Settings.msgShopTypeDisabled, "{type}", shopType.getIdentifier());
			return;
		}

		// can the selected shop object type be used?
		if (!shopObjType.hasPermission(player)) {
			Utils.sendMessage(player, Settings.msgNoPermission);
			return;
		}
		if (!shopObjType.isEnabled()) {
			Utils.sendMessage(player, Settings.msgShopObjectTypeDisabled, "{type}", shopObjType.getIdentifier());
			return;
		}

		// default: spawn on top of targeted block:
		BlockFace targetBlockFace = BlockFace.UP;
		if (!shopObjType.isValidSpawnBlockFace(targetBlock, targetBlockFace)) {
			// some object types (signs) may allow placement on the targeted side:
			targetBlockFace = Utils.getTargetBlockFace(player, targetBlock);
			if (targetBlockFace == null || !shopObjType.isValidSpawnBlockFace(targetBlock, targetBlockFace)) {
				// invalid targeted block face:
				Utils.sendMessage(player, Settings.msgShopCreateFail);
				return;
			}
		}
		Block spawnBlock = targetBlock.getRelative(targetBlockFace);
		// check if the shop can be placed there (enough space, etc.):
		if (!shopObjType.isValidSpawnBlock(spawnBlock)) {
			// invalid spawn location:
			Utils.sendMessage(player, Settings.msgShopCreateFail);
			return;
		}
		Location spawnLocation = spawnBlock.getLocation();

		if (!shopkeeperRegistry.getShopkeepersAtLocation(spawnLocation).isEmpty()) {
			// there is already a shopkeeper at that location:
			Utils.sendMessage(player, Settings.msgShopCreateFail);
			return;
		}

		// shop creation data:
		ShopCreationData shopCreationData;
		if (createPlayerShop) {
			// create player shopkeeper:
			shopCreationData = PlayerShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace, player, targetBlock);
		} else {
			// create admin shopkeeper:
			shopCreationData = ShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace);
		}
		assert shopCreationData != null;

		// create shopkeeper:
		plugin.handleShopkeeperCreation(shopCreationData);
	}
}
