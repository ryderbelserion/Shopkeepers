package com.nisovin.shopkeepers.util.java;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A mutable wrapper for a {@code long} value.
 */
public final class MutableLong implements Comparable<Number> {

	private long value;

	/**
	 * Creates a new {@link MutableLong} with a value of {@code 0}.
	 */
	public MutableLong() {
	}

	/**
	 * Creates a new {@link MutableLong}
	 * 
	 * @param value
	 *            the value
	 */
	public MutableLong(long value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public long getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(long value) {
		this.value = value;
	}

	/**
	 * Increments the value.
	 * 
	 * @param amount
	 *            the amount to increment
	 */
	public void increment(long amount) {
		this.value += amount;
	}

	/**
	 * Decrements the value.
	 * 
	 * @param amount
	 *            the amount to decrement
	 */
	public void decrement(long amount) {
		this.value -= amount;
	}

	@Override
	public int compareTo(Number o) {
		return Long.compare(value, o.longValue());
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MutableLong)) return false;
		MutableLong other = (MutableLong) obj;
		return value == other.value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MutableLong [value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
