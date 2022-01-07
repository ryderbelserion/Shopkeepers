package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.EntityArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityArgument.TargetEntityFilter;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityFallback;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.ui.villagerEditor.VillagerEditorHandler;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;

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
			if (ShopkeeperMetadata.isTagged(entity) || CitizensUtils.isNPC(entity)) {
				return false; // Shopkeeper or Citizens NPC.
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
