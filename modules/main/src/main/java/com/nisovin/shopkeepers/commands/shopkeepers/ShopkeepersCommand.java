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

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
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
import com.nisovin.shopkeepers.commands.shopkeepers.snapshot.CommandSnapshot;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

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
		childCommands.register(new CommandNotify());
		childCommands.register(new CommandList(shopkeeperRegistry));
		childCommands.register(new CommandRemove(confirmations));
		childCommands.register(new CommandRemoveAll(plugin, shopkeeperRegistry, confirmations));
		childCommands.register(new CommandGive());
		childCommands.register(new CommandGiveCurrency());
		childCommands.register(new CommandConvertItems());
		childCommands.register(new CommandRemote());
		childCommands.register(new CommandEdit());
		childCommands.register(new CommandTransfer());
		childCommands.register(new CommandSetTradePerm());
		childCommands.register(new CommandSetForHire());
		childCommands.register(new CommandSnapshot(confirmations));
		childCommands.register(new CommandEditVillager());
		// Hidden commands:
		childCommands.register(new CommandConfirm(confirmations));
		// Hidden debugging / utility commands:
		childCommands.register(new CommandCleanupCitizenShopkeepers());
		childCommands.register(new CommandCheck(plugin));
		childCommands.register(new CommandCheckItem());
		childCommands.register(new CommandYaml());
		childCommands.register(new CommandDebugCreateShops(plugin));
		childCommands.register(new CommandTestDamage(plugin));
		childCommands.register(new CommandTestSpawn(plugin));
	}

	// This also hides the command from the help page if the player shop creation via command is disabled.
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
		// If the player is underwater or inside lava, we ignore fluid collisions to allow the placement of shopkeepers
		// underwater or inside lava. Otherwise, we take fluids into account, which allows the placement of shopkeepers
		// on top of water or lava, such as for example for striders.
		Block playerBlock = player.getEyeLocation().getBlock();
		boolean ignoreFluids = playerBlock.isLiquid();
		FluidCollisionMode fluidCollisionMode = ignoreFluids ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
		// This takes passable blocks (including fluids, depending on the collision mode) into account (i.e. collides
		// with them):
		RayTraceResult targetBlockInfo = player.rayTraceBlocks(10.0D, fluidCollisionMode);

		// Check for valid targeted block:
		if (targetBlockInfo == null) {
			TextUtils.sendMessage(player, Messages.mustTargetBlock);
			return;
		}
		Block targetBlock = targetBlockInfo.getHitBlock();
		BlockFace targetBlockFace = targetBlockInfo.getHitBlockFace();
		assert targetBlock != null && !targetBlock.isEmpty();
		assert targetBlockFace != null;

		ShopType<?> shopType = context.get(ARGUMENT_SHOP_TYPE);
		ShopObjectType<?> shopObjType = context.get(ARGUMENT_OBJECT_TYPE);

		// We use different defaults depending on whether the player might be trying to create a player or admin shop:
		boolean containerTargeted = ItemUtils.isContainer(targetBlock.getType());
		boolean maybeCreatePlayerShop = containerTargeted;
		if (maybeCreatePlayerShop) {
			// Default shop type and shop object type: First usable player shop type and shop object type.
			if (shopType == null) {
				shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
			}
		} else {
			// Default shop type and shop object type:
			if (shopType == null) {
				shopType = DefaultShopTypes.ADMIN_REGULAR();
				// Note: Shop type permissions are checked afterwards during shop creation.
			}
		}

		if (shopObjType == null) {
			shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);
		}
		if (shopType == null || shopObjType == null) {
			// The player cannot create shops at all:
			TextUtils.sendMessage(player, Messages.noPermission);
			return;
		}
		assert shopType != null && shopObjType != null;
		boolean isPlayerShopType = (shopType instanceof PlayerShopType);

		if (isPlayerShopType) {
			if (!Settings.createPlayerShopWithCommand) {
				TextUtils.sendMessage(player, Messages.noPlayerShopsViaCommand);
				return;
			} else if (!containerTargeted) {
				TextUtils.sendMessage(player, Messages.mustTargetContainer);
				return;
			}
			// Note: We check if the targeted container is valid / supported during shop creation.
		}

		// Determine spawn location:
		Location spawnLocation = plugin.getShopkeeperCreation().determineSpawnLocation(player, targetBlock, targetBlockFace);

		// Shop creation data:
		ShopCreationData shopCreationData;
		if (isPlayerShopType) {
			// Create player shopkeeper:
			shopCreationData = PlayerShopCreationData.create(player, (PlayerShopType<?>) shopType, shopObjType, spawnLocation, targetBlockFace, targetBlock);
		} else {
			// Create admin shopkeeper:
			shopCreationData = AdminShopCreationData.create(player, (AdminShopType<?>) shopType, shopObjType, spawnLocation, targetBlockFace);
		}
		assert shopCreationData != null;

		// Handle shopkeeper creation:
		plugin.handleShopkeeperCreation(shopCreationData);
	}
}
