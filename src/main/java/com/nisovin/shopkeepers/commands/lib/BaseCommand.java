package com.nisovin.shopkeepers.commands.lib;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class BaseCommand extends Command implements org.bukkit.command.CommandExecutor, TabCompleter {

	public BaseCommand(org.bukkit.command.PluginCommand bukkitCommand) {
		super(bukkitCommand.getName(), bukkitCommand.getAliases());
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
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String usedAlias, String[] args) {
		CommandInput input = new CommandInput(sender, command.getName(), usedAlias);
		CommandContext commandContext = new CommandContext();
		CommandArgs commandArgs = new CommandArgs(args);
		try {
			this.handleCommand(input, commandContext, commandArgs);
		} catch (CommandException e) {
			TextUtils.sendMessage(sender, e.getMessage());
		} catch (Exception e) {
			// an unexpected exception was caught:
			TextUtils.sendMessage(sender, ChatColor.RED + "An error occurred during command handling! Check the console log.");
			Log.severe("An error occurred during command handling!", e);
			Log.severe(commandContext.toString());
		}

		// we completely handle the command, including printing usage or help on syntax failure:
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String usedAlias, String[] args) {
		CommandInput input = new CommandInput(sender, command.getName(), usedAlias);
		return this.handleTabCompletion(input, new CommandContext(), new CommandArgs(args));
	}
}
