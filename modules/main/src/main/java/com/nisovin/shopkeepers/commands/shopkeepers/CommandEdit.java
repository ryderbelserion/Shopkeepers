package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.lang.Messages;

class CommandEdit extends PlayerCommand {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandEdit() {
		super("edit");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.REMOTE_EDIT_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionRemoteEdit);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR())),
				TargetShopkeeperFilter.ANY
		));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		Shopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);

		// Try to open the shop editor window:
		// Fails if the user does not have the permission to edit the shop.
		shopkeeper.openEditorWindow(player);
	}
}
