package com.nisovin.shopkeepers.util.java;

import java.util.LinkedHashMap;
import java.util.Map;

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
	public static <A, B> Map<A, B> toMap(Pair<A, B>... pairs) {
		Map<A, B> map = new LinkedHashMap<>();
		for (Pair<A, B> pair : pairs) {
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
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof Pair<?, ?>)) return false;
		Pair<?, ?> other = (Pair<?, ?>) o;
		Object otherFirst = other.getFirst();
		if (first == null ? otherFirst != null : !first.equals(otherFirst)) {
			return false;
		}
		Object otherSecond = other.getSecond();
		if (second == null ? otherSecond != null : !second.equals(otherSecond)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) + 17 * (second == null ? 0 : second.hashCode());
	}

	@Override
	public String toString() {
		return new StringBuilder().append('(')
				.append(first)
				.append(',').append(second)
				.append(')').toString();
	}
}
