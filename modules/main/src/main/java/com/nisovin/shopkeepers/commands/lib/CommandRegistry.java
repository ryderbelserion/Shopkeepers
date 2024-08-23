package com.nisovin.shopkeepers.commands.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

public class CommandRegistry {

	private final Command parent;

	// Sorted by insertion order:
	private final Set<Command> commands = new LinkedHashSet<>();
	private final Set<Command> commandsView = Collections.unmodifiableSet(commands);
	// Normalized aliases:
	// Implementation detail (used by Command): All aliases for the same command are stored in
	// succession.
	private final Map<String, Command> commandsByAlias = new LinkedHashMap<>();
	private final Map<String, Command> commandsByAliasView = Collections.unmodifiableMap(commandsByAlias);

	public CommandRegistry(@UnknownInitialization Command parent) {
		Validate.notNull(parent, "parent is null");
		this.parent = Unsafe.initialized(parent);
	}

	public Command getParent() {
		return parent;
	}

	public void register(Command command) {
		Validate.notNull(command, "command is null");
		Validate.isTrue(command.getParent() == null, "command has already been registered somewhere");
		Validate.isTrue(!commands.contains(command), "command is already registered");

		// Register command by name:
		String name = CommandUtils.normalize(command.getName());
		Validate.isTrue(!commandsByAlias.containsKey(name),
				"Another command with this name is already registered: " + name);
		commandsByAlias.put(name, command);

		// Register command aliases:
		for (String alias : command.getAliases()) {
			alias = CommandUtils.normalize(alias);
			// Only register the alias, if it is not yet mapped to another command:
			commandsByAlias.putIfAbsent(alias, command);
		}

		// Register command:
		commands.add(command);

		// Set parent command:
		command.setParent(parent);
	}

	public boolean isRegistered(Command command) {
		return commands.contains(command);
	}

	public void unregister(Command command) {
		Validate.notNull(command, "command is null");
		Validate.isTrue(command.getParent() == parent, "command is registered somewhere else");
		Validate.isTrue(commands.contains(command), "command is not registered here");

		// Unregister by name:
		String name = CommandUtils.normalize(command.getName());
		assert commandsByAlias.get(name) == command;
		commandsByAlias.remove(name);

		// Unregister aliases:
		for (String alias : command.getAliases()) {
			alias = CommandUtils.normalize(alias);
			// Only remove the mapping, if the alias is currently mapped to this command:
			commandsByAlias.remove(alias, command);
		}

		// Unregister command:
		commands.remove(command);

		// Unset parent command:
		command.setParent(null);
	}

	/**
	 * Gets all registered commands.
	 * 
	 * @return an unmodifiable view on all registered commands
	 */
	public Collection<? extends Command> getCommands() {
		return commandsView;
	}

	/**
	 * Gets the {@link Command} mapped to the given alias.
	 * 
	 * @param alias
	 *            the alias
	 * @return the command, or <code>null</code>
	 */
	public @Nullable Command getCommand(String alias) {
		Validate.notNull(alias, "alias is null");
		return commandsByAlias.get(CommandUtils.normalize(alias));
	}

	/**
	 * Gets all registered command aliases.
	 * <p>
	 * The aliases are {@link CommandUtils#normalize(String) normalized}, ordered by insertion and
	 * grouped by command.
	 * 
	 * @return an unmodifiable view on all registered command aliases
	 */
	public Set<? extends String> getAliases() {
		return commandsByAliasView.keySet();
	}

	/**
	 * Gets all registered command aliases and the commands they are mapped to.
	 * 
	 * @return an unmodifiable view on all registered aliases and the commands they are mapped to
	 */
	public Map<? extends String, ? extends Command> getAliasesMap() {
		return commandsByAliasView;
	}

	/**
	 * Gets all registered aliases that are mapped to the specified command.
	 * 
	 * @param command
	 *            the command
	 * @return an unmodifiable view on all registered aliases that are mapped to the specified
	 *         command
	 */
	public List<? extends String> getAliases(Command command) {
		if (commandsByAlias.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> aliases = new ArrayList<>();
		for (Entry<? extends String, ? extends Command> entry : commandsByAlias.entrySet()) {
			if (entry.getValue() == command) {
				aliases.add(entry.getKey());
			}
		}
		return Collections.unmodifiableList(aliases);
	}
}
