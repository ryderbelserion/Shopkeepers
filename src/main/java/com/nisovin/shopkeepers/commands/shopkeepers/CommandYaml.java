package com.nisovin.shopkeepers.commands.shopkeepers;

import java.io.PrintStream;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemUtils;

class CommandYaml extends PlayerCommand {

	private static final int MAX_OUTPUT_LENGTH = 15;

	CommandYaml() {
		super("yaml");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Prints the YAML representation of the held item."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();

		ItemStack itemInHand = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(itemInHand)) {
			player.sendMessage(ChatColor.GRAY + "No item in hand!");
			return;
		}

		// Serialized ItemStack and ItemData:
		String[] serializedItemStack = ConfigUtils.getYAMLOutput(itemInHand);
		String[] serializedItemData = ConfigUtils.getYAMLOutput(new ItemData(itemInHand).serialize());

		// Print to player:
		player.sendMessage(ChatColor.YELLOW + "Serialized ItemStack:");
		if (serializedItemStack.length > MAX_OUTPUT_LENGTH) {
			player.sendMessage(ChatColor.GRAY + "The output is too large for the chat.");
		} else {
			for (String line : serializedItemStack) {
				player.sendMessage(line);
			}
		}

		player.sendMessage(ChatColor.YELLOW + "Config item data:");
		if (serializedItemData.length > MAX_OUTPUT_LENGTH) {
			player.sendMessage(ChatColor.GRAY + "The output is too large for the chat.");
		} else {
			for (String line : serializedItemData) {
				player.sendMessage(line);
			}
		}

		player.sendMessage(ChatColor.GRAY + "Note: The output gets also logged to the console for easier copying.");

		// Print to console:
		PrintStream stream = System.out;
		synchronized (stream) { // Assumes the implementation synchronizes on itself.
			stream.println("Serialized ItemStack:");
			for (String line : serializedItemStack) {
				stream.println(line);
			}
			stream.println("Config item data:");
			for (String line : serializedItemData) {
				stream.println(line);
			}
		}
	}
}
