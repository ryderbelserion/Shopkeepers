package com.nisovin.shopkeepers.commands.lib.context;

import java.util.Map;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Stores information that is relevant for a particular command execution, such as parsed command
 * arguments.
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
	 * Gets the value for the given key.
	 * <p>
	 * The value is expected to be present.
	 * 
	 * @param <T>
	 *            the expected type of value
	 * @param key
	 *            the key
	 * @return the value, not <code>null</code>
	 * @throws IllegalStateException
	 *             if there is no value for the given key
	 */
	public <T> @NonNull T get(String key);

	/**
	 * Gets the value for the given key, or returns <code>null</code> if there is no such value.
	 * 
	 * @param <T>
	 *            the expected type of value
	 * @param key
	 *            the key
	 * @return the value, or <code>null</code> if there is no value for the given key
	 */
	public <T> @Nullable T getOrNull(String key);

	/**
	 * Gets the value for the given key, or returns the specified default value if there is no such
	 * value.
	 * 
	 * @param <T>
	 *            the expected type of value
	 * @param key
	 *            the key
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the value, or the given default value if there is no value for the given key
	 */
	public <T> @Nullable T getOrDefault(String key, @Nullable T defaultValue);

	/**
	 * Gets the value for the given key, or returns the default value provided by the given
	 * {@link Supplier} if there is no such value.
	 * 
	 * @param <T>
	 *            the expected type of value
	 * @param key
	 *            the key
	 * @param defaultValueSupplier
	 *            the default value supplier, not <code>null</code>
	 * @return the value, or the supplied default value if there is no value for the given key (can
	 *         be <code>null</code>)
	 */
	public <T> @Nullable T getOrDefault(String key, Supplier<@Nullable T> defaultValueSupplier);

	/**
	 * Checks if this {@link CommandContext} contains a value for the given key.
	 * 
	 * @param key
	 *            the key
	 * @return <code>true</code> if a value is available
	 */
	public boolean has(String key);

	/**
	 * Gets an unmodifiable map view on the contents of this command context.
	 * 
	 * @return an unmodifiable map view on the contents of this command context
	 */
	public Map<? extends String, @NonNull ?> getMapView();

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

	// Note on hashCode and equals: Contexts are compared by identity, which is quick and sufficient
	// for our needs. Content based comparisons can be achieved via #getMapView().
}
