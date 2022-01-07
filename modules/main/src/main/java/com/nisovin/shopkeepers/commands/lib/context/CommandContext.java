package com.nisovin.shopkeepers.commands.lib.context;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Stores parsed command arguments.
 */
public interface CommandContext {

	/**
	 * Stores a value for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param value
	 *            the value, not <code>null</code>
	 */
	public void put(String key, Object value);

	/**
	 * Retrieves a stored value for the given key.
	 * 
	 * @param <T>
	 *            the value type
	 * @param key
	 *            the key
	 * @return the value, <code>null</code> if no value is stored for the given key
	 */
	public <T> T get(String key);

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
	public <T> T getOrDefault(String key, T defaultValue);

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
	public <T> T getOrDefault(String key, Supplier<T> defaultValueSupplier);

	/**
	 * Checks if a value is stored for the given key.
	 * 
	 * @param key
	 *            the key
	 * @return <code>true</code> if a value is available
	 */
	public boolean has(String key);

	/**
	 * Gets an unmodifiable map view on the contents of the command context.
	 * 
	 * @return an unmodifiable map view on the contents of the command context
	 */
	public Map<String, Object> getMapView();

	/**
	 * Gets an unmodifiable view on this command context.
	 * 
	 * @return an unmodifiable view on this command context
	 */
	public CommandContextView getView();

	/**
	 * Creates a (shallow) copy of the {@link CommandContext}.
	 * 
	 * @return the copied command context
	 */
	public CommandContext copy();

	// Note on hashCode and equals: Contexts are compared by identity, which is quick and sufficient for our needs.
	// Content based comparison can be achieved via #getMapView().
}
