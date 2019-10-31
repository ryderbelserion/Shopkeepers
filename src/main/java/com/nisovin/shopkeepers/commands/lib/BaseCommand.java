package com.nisovin.shopkeepers.commands.lib;

import java.util.List;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

/**
 * Implements the {@link CommandExecutor} and {@link TabCompleter} for a Bukkit {@link PluginCommand} by invoking our
 * command handling.
 */
public abstract class BaseCommand extends Command implements org.bukkit.command.CommandExecutor, TabCompleter {

	/**
	 * Creates a new {@link BaseCommand} that configures itself for use by the given {@link PluginCommand}.
	 * <p>
	 * This adopts the plugin command's name, aliases, description and permission and sets itself up as the plugin
	 * command's {@link CommandExecutor} and {@link TabCompleter}.
	 * 
	 * @param bukkitCommand
	 *            the corresponding bukkit command
	 */
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
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command bukkitCommand, String commandAlias, String[] args) {
		CommandInput input = new CommandInput(sender, this, commandAlias, args);
		this.handleCommand(input);
		// we completely handle the command, including printing usage or help on syntax failure:
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command bukkitCommand, String commandAlias, String[] args) {
		CommandInput input = new CommandInput(sender, this, commandAlias, args);
		return this.handleTabCompletion(input);
	}
}
