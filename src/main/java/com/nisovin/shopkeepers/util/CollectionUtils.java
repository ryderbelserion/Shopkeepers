package com.nisovin.shopkeepers.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class CollectionUtils {

	private CollectionUtils() {
	}

	// shortcut map initializers:

	// maximum capacity of a HashMap (largest power of two fitting into an int)
	private static final int MAX_CAPACITY = (1 << 30);

	// capacity for a HashMap with the specified expected size and a loading-factor of >= 0.75,
	// that prevents the map from resizing
	private static int capacity(int expectedSize) {
		assert expectedSize >= 0;
		if (expectedSize < 3) {
			return expectedSize + 1;
		}
		if (expectedSize < MAX_CAPACITY) {
			return (int) ((float) expectedSize / 0.75F + 1.0F);
		}
		return Integer.MAX_VALUE;
	}

	public static <K, V> Map<K, V> createMap(K key, V value) {
		Map<K, V> map = new LinkedHashMap<>(capacity(1));
		map.put(key, value);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2) {
		Map<K, V> map = new LinkedHashMap<>(capacity(2));
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3) {
		Map<K, V> map = new LinkedHashMap<>(capacity(3));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
		Map<K, V> map = new LinkedHashMap<>(capacity(4));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
		Map<K, V> map = new LinkedHashMap<>(capacity(5));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		map.put(key5, value5);
		return map;
	}
}
