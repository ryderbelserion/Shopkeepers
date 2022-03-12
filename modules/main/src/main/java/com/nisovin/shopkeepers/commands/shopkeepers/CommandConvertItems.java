package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerFallback;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.itemconversion.ItemConversion;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

class CommandConvertItems extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_ALL = "all";

	CommandConvertItems() {
		super("convertItems", Arrays.asList("convertItem", "convert"));

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionConvertItems);

		// Arguments:
		this.addArgument(new SenderPlayerFallback(new PlayerArgument(ARGUMENT_PLAYER)));
		this.addArgument(new LiteralArgument(ARGUMENT_ALL).optional());
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OTHERS_PERMISSION);
	}

	// TODO Add a per-player timeout to prevent command spamming? Because this command might be performance heavy.

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		assert targetPlayer != null;
		boolean targetSelf = (sender.equals(targetPlayer));

		boolean convertAll = context.has(ARGUMENT_ALL);

		// Check permission:
		if (targetSelf) {
			this.checkPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OWN_PERMISSION);
		} else {
			this.checkPermission(sender, ShopkeepersPlugin.CONVERT_ITEMS_OTHERS_PERMISSION);
		}

		PlayerInventory inventory = targetPlayer.getInventory();
		int convertedStacks = 0;
		// Note: This command converts all items, regardless of the 'convert-player-items' and related settings.
		if (convertAll) {
			// Handles content, armor and off hand items, cursor item, and inventory updating:
			long startNanos = System.nanoTime();
			convertedStacks = ItemConversion.convertItems(inventory, (item) -> true, true);
			long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
			final int finalConvertedStacks = convertedStacks;
			// Note: The conversion always has some performance impact, even if no items got actually converted. We
			// therefore always print the debug messages to allow debugging the item conversion times.
			Log.debug(DebugOptions.itemConversions,
					() -> "Converted " + finalConvertedStacks + " item stacks in the inventory of player '"
							+ targetPlayer.getName() + "' (took " + durationMillis + " ms)."
			);
		} else {
			// Only convert the held item:
			ItemStack itemInHand = inventory.getItemInMainHand();
			if (!ItemUtils.isEmpty(itemInHand)) {
				long startNanos = System.nanoTime();
				ItemStack convertedItem = ItemConversion.convertItem(itemInHand);
				long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
				if (!itemInHand.isSimilar(convertedItem)) {
					convertedStacks = 1;
					inventory.setItemInMainHand(convertedItem);
					targetPlayer.updateInventory();
				}
				// Note: The conversion always has some performance impact, even if no items got actually converted. We
				// therefore always print the debug messages to allow debugging the item conversion times.
				Log.debug(DebugOptions.itemConversions,
						() -> "Converted the held item stack of player '" + targetPlayer.getName()
								+ "' (took " + durationMillis + " ms)."
				);
			}
		}

		// Inform target player:
		TextUtils.sendMessage(targetPlayer, Messages.itemsConverted,
				"count", convertedStacks
		);
		if (!targetSelf) {
			// Inform command executor:
			TextUtils.sendMessage(sender, Messages.itemsConverted,
					"count", convertedStacks
			);
		}
	}
}
