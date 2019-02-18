package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

class CommandGive extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_AMOUNT = "amount";

	private final PlayerArgument playerArgument = new PlayerArgument(ARGUMENT_PLAYER);

	CommandGive() {
		super("give");

		// set permission:
		this.setPermission(ShopkeepersPlugin.GIVE_PERMISSION);

		// set description:
		this.setDescription(Settings.msgCommandDescriptionGive);

		// arguments:
		this.addArgument(new OptionalArgument(playerArgument));
		this.addArgument(new OptionalArgument(new PositiveIntegerArgument(ARGUMENT_AMOUNT)));
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		CommandSender sender = input.getSender();

		Player targetPlayer = context.get(ARGUMENT_PLAYER);
		if (targetPlayer == null) {
			if (!(sender instanceof Player)) {
				// the argument is only optional if the sender is a player:
				throw new ArgumentParseException(playerArgument.getMissingArgumentErrorMsg());
			}
			targetPlayer = (Player) sender;
		}
		assert targetPlayer != null;

		Integer amount = context.get(ARGUMENT_AMOUNT);
		if (amount == null) amount = 1;
		assert amount >= 1;
		// upper limit to avoid accidental misuse:
		if (amount > 1024) amount = 1024;

		ItemStack item = Settings.createShopCreationItem();
		item.setAmount(amount);

		PlayerInventory inventory = targetPlayer.getInventory();
		ItemStack[] contents = inventory.getStorageContents();
		int remaining = ItemUtils.addItems(contents, item);
		inventory.setStorageContents(contents);
		if (remaining > 0) {
			item.setAmount(remaining);
			targetPlayer.getWorld().dropItem(targetPlayer.getEyeLocation(), item);
		}

		// success:
		Utils.sendMessage(sender, Settings.msgShopCreationItemsGiven,
				"{player}", targetPlayer.getDisplayName(),
				"{amount}", String.valueOf(amount));
	}
}
