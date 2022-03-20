package com.nisovin.shopkeepers.util.java;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class MapUtils {

	// Maximum capacity of a HashMap (largest power of two fitting into an int):
	private static final int MAX_CAPACITY = (1 << 30);

	// Capacity for a HashMap/Set with the specified expected size and a load factor of >= 0.75 that
	// prevents the Map/Set from resizing.
	public static int getIdealHashMapCapacity(int expectedSize) {
		Validate.isTrue(expectedSize >= 0, "expectedSize cannot be negative");
		if (expectedSize < 3) {
			return expectedSize + 1;
		}
		if (expectedSize < MAX_CAPACITY) {
			return (int) ((float) expectedSize / 0.75F + 1.0F);
		}
		return Integer.MAX_VALUE;
	}

	// Shortcut map initializers:

	public static <K, V> Map<K, V> createMap(K key, V value) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(1));
		map.put(key, value);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(2));
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(3));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	public static <K, V> Map<K, V> createMap(
			K key1, V value1,
			K key2, V value2,
			K key3, V value3,
			K key4, V value4
	) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(4));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		return map;
	}

	public static <K, V> Map<K, V> createMap(
			K key1, V value1,
			K key2, V value2,
			K key3, V value3,
			K key4, V value4,
			K key5, V value5
	) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(5));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		map.put(key5, value5);
		return map;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> createMap(Object... keyValuePairs) {
		Map<K, V> map = new LinkedHashMap<>(getIdealHashMapCapacity(keyValuePairs.length / 2));
		final int keyLimit = keyValuePairs.length - 1;
		for (int i = 0; i < keyLimit; i += 2) {
			Object key = keyValuePairs[i];
			Object value = keyValuePairs[i + 1];
			map.put((K) key, (V) value); // Errors if types don't match the expected ones
		}
		return map;
	}

	public static <K, V> Entry<K, V> entry(K key, V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}

	/**
	 * Creates a new Map that copies the entries of the given Map, but converts all keys
	 * {@link Object#toString() to Strings}.
	 * <p>
	 * If the given Map contains a <code>null</code> key, the resulting Map will contain a
	 * <code>null</code> key as well.
	 * 
	 * @param map
	 *            the map of arbitrary key and value type
	 * @return a new String to Object map, or <code>null</code> if the given Map is
	 *         <code>null</code>
	 */
	public static Map<@Nullable String, @Nullable Object> toStringMap(Map<?, ?> map) {
		Validate.notNull(map, "map is null");
		Map<@Nullable String, @Nullable Object> stringMap = new LinkedHashMap<>(
				getIdealHashMapCapacity(map.size())
		);
		map.forEach((key, value) -> {
			String stringKey = StringUtils.toStringOrNull(key);
			stringMap.put(stringKey, value);
		});
		return stringMap;
	}

	/**
	 * Returns the given Map if it is not <code>null</code>, and otherwise returns an
	 * {@link Collections#emptyMap() empty Map}.
	 * 
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 * @param map
	 *            the map
	 * @return the given map, or an empty map, not <code>null</code>
	 */
	public static <K, V> Map<K, V> getOrEmpty(@Nullable Map<K, V> map) {
		return (map != null) ? map : Collections.emptyMap();
	}

	private MapUtils() {
	}
}
