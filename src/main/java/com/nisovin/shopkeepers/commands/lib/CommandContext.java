package com.nisovin.shopkeepers.commands.lib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.Validate;

/**
 * Stores parsed command arguments.
 */
public final class CommandContext {

	// gets only initialized when used:
	private Map<String, Object> parsedArgs = null;

	/**
	 * Creates a new and empty {@link CommandContext}.
	 */
	public CommandContext() {
	}

	/**
	 * Creates a new {@link CommandContext} and fills it with the current values from the given {@link CommandContext}.
	 * 
	 * @param otherContext
	 *            another {@link CommandContext}
	 */
	public CommandContext(CommandContext otherContext) {
		if (otherContext != null && otherContext.parsedArgs != null) {
			this.parsedArgs = new LinkedHashMap<>(parsedArgs);
		}
	}

	/**
	 * Stores a value for the given key.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value, not <code>null</code>
	 */
	public void put(String key, Object value) {
		Validate.notEmpty(key, "Key is empty!");
		Validate.notNull(value, "Value is null!");
		if (parsedArgs == null) {
			parsedArgs = new LinkedHashMap<>();
		}
		parsedArgs.put(key, value);
	}

	/**
	 * Retrieves a stored value for the given key.
	 * 
	 * @param <T>
	 *            the value type
	 * @param key
	 *            the key
	 * @return the value, <code>null</code> if no value is stored for the given key
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		if (parsedArgs == null) {
			return null;
		}
		return (T) parsedArgs.get(key);
	}

	/**
	 * Gets the stored value for the given key, or returns the specified default value in case there is no value stored.
	 * 
	 * @param <T>
	 *            the value type
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the default value
	 * @return the stored value, or the default value (can be <code>null</code> if the default value is
	 *         <code>null</code>)
	 */
	public <T> T getOrDefault(String key, T defaultValue) {
		T value = this.get(key);
		return (value != null ? value : defaultValue);
	}

	/**
	 * Gets the stored value for the given key, or returns a default value created by the given {@link Supplier} in case
	 * there is no value stored.
	 * 
	 * @param <T>
	 *            the value type
	 * @param key
	 *            the key
	 * @param defaultValueSupplier
	 *            the default value supplier
	 * @return the stored value, or the default value (can be <code>null</code> if the default value supplier is
	 *         <code>null</code> or returned <code>null</code>)
	 */
	public <T> T getOrDefault(String key, Supplier<T> defaultValueSupplier) {
		T value = this.get(key);
		if (value != null) return value;
		if (defaultValueSupplier == null) return null;
		return defaultValueSupplier.get();
	}

	/**
	 * Checks if a value is stored for the given key.
	 * 
	 * @param key
	 *            the key
	 * @return <code>true</code> if a value is available
	 */
	public boolean has(String key) {
		if (parsedArgs == null) {
			return false;
		}
		return parsedArgs.containsKey(key);
	}
}
