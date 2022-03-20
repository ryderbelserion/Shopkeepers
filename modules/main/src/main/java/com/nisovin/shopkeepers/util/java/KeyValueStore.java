package com.nisovin.shopkeepers.util.java;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A key-value store.
 * <p>
 * This type is similar to a {@link Map} with non-empty {@link String} keys and arbitrarily typed
 * non-<code>null</code> values, but only supports a very limited subset of operations.
 */
public interface KeyValueStore {

	/**
	 * Gets the value for the specified key.
	 * <p>
	 * The given type parameter is unchecked. Trying to assign the returned value to a variable of
	 * an assignment incompatible type will result in a {@link ClassCastException}.
	 * 
	 * @param <T>
	 *            the expected type of the value (unchecked)
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the value, or <code>null</code> if there is no value for the specified key
	 */
	public <T> @Nullable T get(String key);

	/**
	 * Stores the given value under the specified key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param value
	 *            the value, or <code>null</code> to clear any previous value for the given key
	 */
	public void set(String key, @Nullable Object value);
}
