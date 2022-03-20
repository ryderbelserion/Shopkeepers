package com.nisovin.shopkeepers.util.java;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable pair of values.
 * 
 * @param <V1>
 *            the type of the first value
 * @param <V2>
 *            the type of the second value
 */
public class Pair<V1, V2> {

	/**
	 * Creates a {@link Pair} of the given values.
	 * 
	 * @param <V1>
	 *            the type of the first value
	 * @param <V2>
	 *            the type of the second value
	 * @param first
	 *            the first value
	 * @param second
	 *            the second value
	 * @return the pair
	 */
	public static <V1, V2> Pair<V1, V2> of(V1 first, V2 second) {
		return new Pair<>(first, second);
	}

	@SafeVarargs
	public static <V1, V2> Map<V1, V2> toMap(Pair<V1, V2>... pairs) {
		Map<V1, V2> map = new LinkedHashMap<>();
		for (Pair<V1, V2> pair : pairs) {
			map.put(pair.getFirst(), pair.getSecond());
		}
		return map;
	}

	private final V1 first;
	private final V2 second;

	protected Pair(V1 first, V2 second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Gets the first value.
	 * 
	 * @return the first value
	 */
	public V1 getFirst() {
		return first;
	}

	/**
	 * Gets the second value.
	 * 
	 * @return the second value
	 */
	public V2 getSecond() {
		return second;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Pair<?, ?>)) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (!Objects.equals(first, other.getFirst())) return false;
		if (!Objects.equals(second, other.getSecond())) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(first) + 17 * Objects.hashCode(second);
	}

	@Override
	public String toString() {
		return new StringBuilder().append('(')
				.append(first)
				.append(',').append(second)
				.append(')').toString();
	}
}
