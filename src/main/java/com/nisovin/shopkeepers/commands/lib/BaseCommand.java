package com.nisovin.shopkeepers.commands.lib;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.nisovin.shopkeepers.util.Utils;

public abstract class BaseCommand extends Command implements org.bukkit.command.CommandExecutor, TabCompleter {

	private static List<String> getAliases(org.bukkit.command.PluginCommand bukkitCommand) {
		Validate.notNull(bukkitCommand);
		List<String> aliases = new ArrayList<>();
		aliases.add(bukkitCommand.getName());
		aliases.addAll(bukkitCommand.getAliases());
		return aliases;
	}

	public BaseCommand(org.bukkit.command.PluginCommand bukkitCommand) {
		super(getAliases(bukkitCommand));
		String desc = bukkitCommand.getDescription();
		if (desc != null && !desc.isEmpty()) {
			this.setDescription(desc);
		}
		String permission = bukkitCommand.getPermission();
		if (permission != null && !permission.isEmpty()) {
			this.setPermission(permission);
		}

		// register command executor:
		bukkitCommand.setExecutor(this);
		// register tab completer:
		bukkitCommand.setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
		CommandInput input = new CommandInput(sender, command.getName(), alias);
		try {
			this.handleCommand(input, new CommandContext(), new CommandArgs(args));
		} catch (CommandException e) {
			Utils.sendMessage(sender, e.getMessage());
		} catch (Exception e) {
			// an unexpected exception was caught:
			Utils.sendMessage(sender, ChatColor.RED + "An error occurred during command handling! Check the console log.");
			e.printStackTrace();
		}

		// we completely handle the command, including printing usage or help on syntax failure:
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
		CommandInput input = new CommandInput(sender, command.getName(), alias);
		return this.handleTabCompletion(input, new CommandContext(), new CommandArgs(args));
	}
}
