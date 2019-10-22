package com.nisovin.shopkeepers.commands.lib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.Validate;

/**
 * Stores parsed command arguments.
 */
public class CommandContext {

	// gets only initialized when used:
	protected Map<String, Object> parsedArgs = null;

	/**
	 * Creates a new and empty {@link CommandContext}.
	 */
	public CommandContext() {
	}

	/**
	 * Creates a (shallow) copy of the given {@link CommandContext}.
	 * 
	 * @param otherContext
	 *            the other command context
	 */
	protected CommandContext(CommandContext otherContext) {
		Validate.notNull(otherContext, "The other command context is null!");
		if (otherContext.parsedArgs != null) {
			this.parsedArgs = new LinkedHashMap<>(otherContext.parsedArgs);
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

	/**
	 * Gets an unmodifiable map view on the contents of this command context.
	 * 
	 * @return an unmodifiable map view on the contents of this command context
	 */
	public Map<String, Object> getMapView() {
		if (parsedArgs == null) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(parsedArgs);
		}
	}

	/**
	 * Creates a (shallow) copy of this {@link CommandContext}.
	 * 
	 * @return the copied command context
	 */
	public CommandContext copy() {
		return new CommandContext(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommandContext [parsedArgs=");
		builder.append(parsedArgs);
		builder.append("]");
		return builder.toString();
	}
}
