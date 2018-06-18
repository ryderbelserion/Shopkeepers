package com.nisovin.shopkeepers.command.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.Validate;

public class CommandRegistry {

	private final Command parent;

	// gets only initialized when used, sorted by insertion order:
	// lowercase aliases
	private Map<String, Command> commandsByAlias = null;
	private Map<Command, String> primaryAliases = null;

	public CommandRegistry(Command parent) {
		Validate.notNull(parent);
		this.parent = parent;
	}

	public Command getParent() {
		return parent;
	}

	public void register(Command command) {
		Validate.notNull(command);
		Validate.isTrue(command.getParent() == null, "The given command is already registered somewhere!");

		// lazy initialization:
		if (commandsByAlias == null) {
			commandsByAlias = new LinkedHashMap<>();
			primaryAliases = new LinkedHashMap<>();
		}

		Validate.isTrue(!primaryAliases.containsKey(command), "The given command is already registered!");

		// register aliases:
		String primaryAlias = null;
		for (String alias : command.getAliases()) {
			alias = alias.toLowerCase(Locale.ROOT);
			if (commandsByAlias.containsKey(alias)) {
				// there is already another command for the current alias registered, skip this alias:
				continue;
			}
			commandsByAlias.put(alias, command);
			if (primaryAlias == null) {
				primaryAlias = alias;
			}
		}
		Validate.notNull(primaryAlias, "All aliases for this command are already in use!");

		// register command:
		primaryAliases.put(command, primaryAlias);

		// set parent command:
		command.setParent(parent);
	}

	public boolean isRegistered(Command command) {
		return primaryAliases != null && primaryAliases.containsKey(command);
	}

	public void unregister(Command command) {
		Validate.notNull(command);
		Validate.isTrue(command.getParent() == parent, "The given command is not registered here!");
		Validate.isTrue(primaryAliases.containsKey(command), "The given command is not registered here!");

		// unregister aliases:
		for (String alias : command.getAliases()) {
			alias = alias.toLowerCase(Locale.ROOT);
			if (commandsByAlias.get(alias) == command) {
				// there is another command for the current alias registered, skip this alias:
				continue;
			}
			commandsByAlias.remove(alias);
		}

		// unregister command:
		primaryAliases.remove(command);

		// unset parent command:
		command.setParent(null);
	}

	public Command getCommand(String alias) {
		Validate.notNull(alias);
		if (commandsByAlias == null) {
			return null;
		}
		return commandsByAlias.get(alias.toLowerCase(Locale.ROOT));
	}

	/**
	 * Gets all registered command aliases (in lower-case).
	 * <p>
	 * The aliases are ordered by insertion and grouped by command.
	 * 
	 * @return an unmodifiable view on all registered command aliases
	 */
	public Set<String> getAliases() {
		if (commandsByAlias == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(commandsByAlias.keySet());
	}

	public Map<String, Command> getAliasesMap() {
		if (commandsByAlias == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(commandsByAlias);
	}

	/**
	 * Gets all registered primary aliases (in lower-case).
	 * <p>
	 * One (the first successfully registered) alias per registered command, ordered by insertion.
	 * 
	 * @return an unmodifiable view on all registered primary command aliases
	 */
	public Collection<String> getPrimaryAliases() {
		if (primaryAliases == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableCollection(primaryAliases.values());
	}

	public Map<Command, String> getPrimaryAliasesMap() {
		if (primaryAliases == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(primaryAliases);
	}

	public Set<Command> getCommands() {
		if (primaryAliases == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(primaryAliases.keySet());
	}

	public List<String> getAliases(Command command) {
		if (commandsByAlias == null) {
			return Collections.emptyList();
		}
		List<String> aliases = new ArrayList<>();
		for (Entry<String, Command> entry : commandsByAlias.entrySet()) {
			if (entry.getValue().equals(command)) {
				aliases.add(entry.getKey());
			}
		}
		return aliases;
	}

	public String getPrimaryAlias(Command command) {
		if (primaryAliases == null) {
			return null;
		}
		return primaryAliases.get(command);
	}
}
