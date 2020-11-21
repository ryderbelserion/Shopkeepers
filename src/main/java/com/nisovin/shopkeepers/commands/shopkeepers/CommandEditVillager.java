package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.EntityArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityArgument.TargetEntityFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityFallback;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.ui.defaults.VillagerEditorHandler;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;

/**
 * Opens the villager editor for the targeted (regular) villager or wandering trader.
 */
class CommandEditVillager extends PlayerCommand {

	private static final TargetEntityFilter VILLAGER_FILTER = new TargetEntityFilter() {
		@Override
		public boolean test(Entity entity) {
			if (!(entity instanceof AbstractVillager)) {
				return false; // No villager or wandering trader.
			}
			if (CitizensHandler.isNPC(entity) || ShopkeeperUtils.isShopkeeper(entity)) {
				return false; // NPC or shopkeeper.
			}
			return true;
		}

		@Override
		public Text getNoTargetErrorMsg() {
			return Messages.mustTargetVillager;
		}

		@Override
		public Text getInvalidTargetErrorMsg(Entity entity) {
			return Messages.targetEntityIsNoVillager;
		}
	};

	private static final String ARGUMENT_VILLAGER = "villager";

	CommandEditVillager() {
		super("editVillager");

		// Permission gets checked by testPermission and when opening the editor.

		// Set description:
		this.setDescription(Messages.commandDescriptionEditVillager);

		// Arguments:
		this.addArgument(new TargetEntityFallback(
				new EntityArgument(ARGUMENT_VILLAGER),
				VILLAGER_FILTER
		));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.EDIT_VILLAGERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.EDIT_WANDERING_TRADERS_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		AbstractVillager villager = context.get(ARGUMENT_VILLAGER); // Not null

		// Open the villager editor:
		VillagerEditorHandler villagerEditor = new VillagerEditorHandler(villager);
		SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(villagerEditor, player);
	}
}
