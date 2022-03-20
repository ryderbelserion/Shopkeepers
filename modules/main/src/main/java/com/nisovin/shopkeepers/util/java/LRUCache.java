package com.nisovin.shopkeepers.util.java;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.checkerframework.checker.nullness.qual.Nullable;

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

	private final int maxSize;

	public LRUCache(int maxSize) {
		super(
				MapUtils.getIdealHashMapCapacity(MathUtils.addSaturated(maxSize, 1)),
				0.75f,
				true // LRU ordering
		);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(@Nullable Entry<K, V> eldestEntry) {
		assert eldestEntry != null;
		return (this.size() > maxSize);
	}
}
