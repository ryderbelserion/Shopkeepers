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

import com.nisovin.shopkeepers.util.Validate;

public class CommandRegistry {

	private final Command parent;

	// gets only initialized when used, sorted by insertion order:
	private Set<Command> commands = null;
	// normalized aliases:
	private Map<String, Command> commandsByAlias = null;

	public CommandRegistry(Command parent) {
		Validate.notNull(parent, "Parent is null!");
		this.parent = parent;
	}

	public Command getParent() {
		return parent;
	}

	public void register(Command command) {
		Validate.notNull(command, "Command is null!");
		Validate.isTrue(command.getParent() == null, "The given command is already registered somewhere!");

		// lazy initialization:
		if (commands == null) {
			commands = new LinkedHashSet<>();
			commandsByAlias = new LinkedHashMap<>();
		}

		Validate.isTrue(!commands.contains(command), "The given command is already registered!");

		// register command by name:
		String name = CommandUtils.normalize(command.getName());
		Validate.isTrue(!commandsByAlias.containsKey(name), "Another command is already registered for '" + name + "'!");
		commandsByAlias.put(name, command);

		// register command aliases:
		for (String alias : command.getAliases()) {
			alias = CommandUtils.normalize(alias);
			// only register the alias, if it is not yet mapped to another command:
			commandsByAlias.putIfAbsent(alias, command);
		}

		// register command:
		commands.add(command);

		// set parent command:
		command.setParent(parent);
	}

	public boolean isRegistered(Command command) {
		return commands != null && commands.contains(command);
	}

	public void unregister(Command command) {
		Validate.notNull(command, "Command is null!");
		Validate.isTrue(command.getParent() == parent, "The given command is not registered here!");
		Validate.isTrue(commands.contains(command), "The given command is not registered here!");

		// unregister by name:
		String name = CommandUtils.normalize(command.getName());
		assert commandsByAlias.get(name) == command;
		commandsByAlias.remove(name);

		// unregister aliases:
		for (String alias : command.getAliases()) {
			alias = CommandUtils.normalize(alias);
			// only remove the mapping, if the alias is currently mapped to this command:
			commandsByAlias.remove(alias, command);
		}

		// unregister command:
		commands.remove(command);

		// unset parent command:
		command.setParent(null);
	}

	/**
	 * Gets all registered commands.
	 * 
	 * @return an unmodifiable view on all registered commands
	 */
	public Collection<Command> getCommands() {
		if (commands == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(commands);
	}

	/**
	 * Gets the {@link Command} mapped to the given alias.
	 * 
	 * @param alias
	 *            the alias
	 * @return the command, or <code>null</code>
	 */
	public Command getCommand(String alias) {
		Validate.notNull(alias, "Alias is null!");
		if (commandsByAlias == null) {
			return null;
		}
		return commandsByAlias.get(CommandUtils.normalize(alias));
	}

	/**
	 * Gets all registered command aliases.
	 * <p>
	 * The aliases are {@link CommandUtils#normalize(String) normalized}, ordered by insertion and grouped by command.
	 * 
	 * @return an unmodifiable view on all registered command aliases
	 */
	public Set<String> getAliases() {
		if (commandsByAlias == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(commandsByAlias.keySet());
	}

	/**
	 * Gets all registered command aliases and the commands they are mapped to.
	 * 
	 * @return an unmodifiable view on all registered aliases and the commands they are mapped to
	 */
	public Map<String, Command> getAliasesMap() {
		if (commandsByAlias == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(commandsByAlias);
	}

	/**
	 * Gets all registered aliases that are mapped to the specified command.
	 * 
	 * @param command
	 *            the command
	 * @return an unmodifiable view on all registered aliases that are mapped to the specified command
	 */
	public List<String> getAliases(Command command) {
		if (commandsByAlias == null) {
			return Collections.emptyList();
		}
		List<String> aliases = new ArrayList<>();
		for (Entry<String, Command> entry : commandsByAlias.entrySet()) {
			if (entry.getValue() == command) {
				aliases.add(entry.getKey());
			}
		}
		return Collections.unmodifiableList(aliases);
	}
}
