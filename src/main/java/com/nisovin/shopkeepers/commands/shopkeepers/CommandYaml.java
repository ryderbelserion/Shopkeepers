package com.nisovin.shopkeepers.commands.shopkeepers;

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
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;

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
		String itemStackYaml = ConfigUtils.toConfigYaml("item-in-hand", itemInHand);
		String itemDataYaml = ConfigUtils.toConfigYaml("item-in-hand-config-data", new ItemData(itemInHand).serialize());

		String[] itemStackYamlLines = StringUtils.splitLines(itemStackYaml);
		String[] itemDataYamlLines = StringUtils.splitLines(itemDataYaml);

		// Print to player:
		player.sendMessage(ChatColor.YELLOW + "Serialized ItemStack:");
		if (itemStackYamlLines.length > MAX_OUTPUT_LENGTH) {
			player.sendMessage(ChatColor.GRAY + "The output is too large for the chat.");
		} else {
			for (String line : itemStackYamlLines) {
				player.sendMessage(line);
			}
		}

		player.sendMessage(ChatColor.YELLOW + "Config item data:");
		if (itemDataYamlLines.length > MAX_OUTPUT_LENGTH) {
			player.sendMessage(ChatColor.GRAY + "The output is too large for the chat.");
		} else {
			for (String line : itemDataYamlLines) {
				player.sendMessage(line);
			}
		}

		player.sendMessage(ChatColor.GRAY + "Note: The output is also logged to the console for easier copying.");

		// Print to console as multi-line messages:
		Log.info("Serialized ItemStack:\n" + itemStackYaml);
		Log.info("Config item data:\n" + itemDataYaml);
	}
}
