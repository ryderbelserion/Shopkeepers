package com.nisovin.shopkeepers.util.java;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link KeyValueStore} that is implemented around a {@link Map}.
 */
public class MapBasedKeyValueStore implements KeyValueStore {

	private final Map<String, Object> map;

	/**
	 * Creates a new {@link MapBasedKeyValueStore} based on a new {@link HashMap}.
	 */
	public MapBasedKeyValueStore() {
		this(new HashMap<>());
	}

	/**
	 * Creates a new {@link MapBasedKeyValueStore} around the given {@link Map}.
	 * <p>
	 * All operations of this {@link MapBasedKeyValueStore} read and write through to the given Map.
	 * 
	 * @param map
	 *            the map to use as key-value storage, not <code>null</code>
	 */
	public MapBasedKeyValueStore(Map<String, Object> map) {
		Validate.notNull(map, "map is null");
		this.map = map;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T get(String key) {
		Validate.notEmpty(key, "key is null or empty");
		return (T) map.get(key);
	}

	@Override
	public void set(String key, @Nullable Object value) {
		Validate.notEmpty(key, "key is null or empty");
		if (value == null) {
			// Clear the previous value for the given key:
			map.remove(key);
		} else {
			map.put(key, value);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MapBasedKeyValueStore [map=");
		builder.append(map);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MapBasedKeyValueStore)) return false;
		MapBasedKeyValueStore other = (MapBasedKeyValueStore) obj;
		if (!map.equals(other.map)) return false;
		return true;
	}
}
