package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Collections;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.arguments.ShopObjectTypeArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopTypeArgument;
import com.nisovin.shopkeepers.commands.lib.BaseCommand;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.CommandRegistry;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class ShopkeepersCommand extends BaseCommand {

	private static final String ARGUMENT_SHOP_TYPE = "shop-type";
	private static final String ARGUMENT_OBJECT_TYPE = "object-type";

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	public ShopkeepersCommand(SKShopkeepersPlugin plugin, PluginCommand pluginCommand, Confirmations confirmations) {
		super(pluginCommand);
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();

		// Permission gets checked by testPermission and during execution.

		// Description:
		this.setDescription(Messages.commandDescriptionShopkeeper);

		// Formatting:
		this.setHelpTitleFormat(Messages.commandHelpTitle.setPlaceholderArguments(
				Collections.singletonMap("version", plugin.getDescription().getVersion())
		));
		this.setHelpUsageFormat(Messages.commandHelpUsageFormat);
		this.setHelpDescFormat(Messages.commandHelpDescriptionFormat);

		// Arguments for shopkeeper creation:
		this.addArgument(new OptionalArgument<>(new ShopTypeArgument(ARGUMENT_SHOP_TYPE)));
		this.addArgument(new OptionalArgument<>(new ShopObjectTypeArgument(ARGUMENT_OBJECT_TYPE)));

		// Register child commands:
		CommandRegistry childCommands = this.getChildCommands();
		childCommands.register(new CommandHelp(this));
		childCommands.register(new CommandReload(plugin));
		childCommands.register(new CommandDebug());
		childCommands.register(new CommandList(shopkeeperRegistry));
		childCommands.register(new CommandRemove(plugin, shopkeeperRegistry, confirmations));
		childCommands.register(new CommandGive());
		childCommands.register(new CommandGiveCurrency());
		childCommands.register(new CommandConvertItems());
		childCommands.register(new CommandRemote());
		childCommands.register(new CommandEdit());
		childCommands.register(new CommandTransfer());
		childCommands.register(new CommandSetTradePerm());
		childCommands.register(new CommandSetForHire());
		childCommands.register(new CommandEditVillager());
		// Hidden commands:
		childCommands.register(new CommandConfirm(confirmations));
		// Hidden debugging / utility commands:
		childCommands.register(new CommandCheck(plugin));
		childCommands.register(new CommandCheckItem());
		childCommands.register(new CommandYaml());
		childCommands.register(new CommandDebugCreateShops(plugin));
	}

	// Also responsible for hiding the command from the help page, if shop creation via command is disabled:
	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return Settings.createPlayerShopWithCommand || PermissionUtils.hasPermission(sender, ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		if (!(input.getSender() instanceof Player)) {
			throw PlayerCommand.createCommandSourceRejectedException(sender);
		}
		Player player = (Player) sender;

		// Creating new shopkeeper:

		// Get targeted block information:
		RayTraceResult targetBlockInfo = player.rayTraceBlocks(10.0D, FluidCollisionMode.NEVER);

		// Check for valid targeted block:
		if (targetBlockInfo == null) {
			TextUtils.sendMessage(player, Messages.shopCreateFail);
			return;
		}
		Block targetBlock = targetBlockInfo.getHitBlock();
		BlockFace targetBlockFace = targetBlockInfo.getHitBlockFace();
		assert targetBlock != null && !targetBlock.isEmpty();
		assert targetBlockFace != null;

		ShopType<?> shopType = context.get(ARGUMENT_SHOP_TYPE);
		ShopObjectType<?> shopObjType = context.get(ARGUMENT_OBJECT_TYPE);

		boolean createPlayerShop = (Settings.createPlayerShopWithCommand && ShopContainers.isSupportedContainer(targetBlock.getType()));
		if (createPlayerShop) {
			// Create player shopkeeper:

			// Default shop type and shop object type: First useable player shop type and shop object type.
			if (shopType == null) {
				shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
			}
			if (shopObjType == null) {
				shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);
			}

			if (shopType == null || shopObjType == null) {
				// The player cannot create shops at all:
				TextUtils.sendMessage(player, Messages.noPermission);
				return;
			}

			// Validate the selected shop type:
			if (!(shopType instanceof PlayerShopType)) {
				// Only player shop types are allowed here:
				TextUtils.sendMessage(player, Messages.noPlayerShopTypeSelected);
				return;
			}
		} else {
			// Create admin shopkeeper:

			// Check permission:
			if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.ADMIN_PERMISSION)) {
				TextUtils.sendMessage(sender, Messages.noPermission);
				return;
			}

			// Default shop type and shop object type:
			if (shopType == null) {
				shopType = DefaultShopTypes.ADMIN();
			}
			if (shopObjType == null) {
				shopObjType = plugin.getDefaultShopObjectType();
			}
			assert shopType != null && shopObjType != null;

			// Validate the selected shop type:
			if (!(shopType instanceof AdminShopType)) {
				// Only admin shop types are allowed here:
				TextUtils.sendMessage(player, Messages.noAdminShopTypeSelected);
				return;
			}
		}
		assert shopType != null && shopObjType != null;

		// Determine spawn location:
		Location spawnLocation = plugin.getShopkeeperCreation().determineSpawnLocation(player, targetBlock, targetBlockFace);

		// Shop creation data:
		ShopCreationData shopCreationData;
		if (createPlayerShop) {
			// Create player shopkeeper:
			shopCreationData = PlayerShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace, targetBlock);
		} else {
			// Create admin shopkeeper:
			shopCreationData = AdminShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace);
		}
		assert shopCreationData != null;

		// Handle shopkeeper creation:
		plugin.handleShopkeeperCreation(shopCreationData);
	}
}
