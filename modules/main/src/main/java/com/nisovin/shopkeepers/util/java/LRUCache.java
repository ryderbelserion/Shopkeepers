package com.nisovin.shopkeepers.util.java;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple least-recently-used cache based on {@link LinkedHashMap}.
 *
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -2682352036845918880L;

	private final int size;

	public LRUCache(int size) {
		super(MapUtils.getIdealHashMapCapacity(size), 0.75f, true); // LRU ordering
		this.size = size;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldestEntry) {
		return (this.size() > size);
	}
}
