package com.nisovin.shopkeepers.commands.lib;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.util.Validate;

/**
 * General information about the command invocation.
 */
public final class CommandInput {

	private final CommandSender sender;
	private final Command command;
	private final String commandAlias;
	private final List<String> arguments; // unmodifiable

	// The arguments are expected to not change during the command processing.
	public CommandInput(CommandSender sender, Command command, String commandAlias, String[] arguments) {
		this(sender, command, commandAlias, (arguments == null) ? Collections.emptyList() : Arrays.asList(arguments));
	}

	// The arguments are expected to not change during the command processing.
	public CommandInput(CommandSender sender, Command command, String commandAlias, List<String> arguments) {
		Validate.notNull(sender, "Sender is null!");
		Validate.notNull(command, "Command is null!");
		Validate.notEmpty(commandAlias, "Command alias is empty!");
		Validate.notNull(arguments, "Arguments is null!");
		Validate.isTrue(!arguments.contains(null), "Arguments contains null!");

		this.sender = sender;
		this.command = command;
		this.commandAlias = commandAlias;
		this.arguments = Collections.unmodifiableList(arguments);
	}

	/**
	 * Gets the {@link CommandSender} which is executing the command.
	 * 
	 * @return the command sender, not <code>null</code>
	 */
	public CommandSender getSender() {
		return sender;
	}

	/**
	 * Gets the {@link Command} that is being executed.
	 * 
	 * @return the command, not <code>null</code>
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Gets the alias that was used to invoke the command.
	 * <p>
	 * Note: The server might allow the execution of the command via aliases that don't match any of the command's
	 * {@link Command#getAliases() known aliases} (for example via the command's known aliases prefixed by the plugin's
	 * name, or via custom aliases that were defined by the server admin).
	 * 
	 * @return the used command alias, not <code>null</code> or empty
	 */
	public String getCommandAlias() {
		return commandAlias;
	}

	/**
	 * Gets the arguments that were specified during the command invocation.
	 * 
	 * @return an unmodifiable view on the arguments, not <code>null</code> but may be empty
	 */
	public List<String> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommandInput [sender=");
		builder.append(sender.getName());
		builder.append(", command=");
		builder.append(command.getName());
		builder.append(", commandAlias=");
		builder.append(commandAlias);
		builder.append(", arguments=");
		builder.append(arguments);
		builder.append("]");
		return builder.toString();
	}
}
