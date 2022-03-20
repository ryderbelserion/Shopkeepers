package com.nisovin.shopkeepers.util.java;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A mutable wrapper around a reference to another object.
 *
 * @param <T>
 *            the type of the stored value
 */
public class Box<T> {

	private @Nullable T value;

	/**
	 * Creates a new {@link Box} with a value of <code>null</code>.
	 */
	public Box() {
	}

	/**
	 * Creates a new {@link Box} with the given value.
	 * 
	 * @param value
	 *            the initial value
	 */
	public Box(@Nullable T value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public @Nullable T getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(@Nullable T value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Box)) return false;
		Box<?> other = (Box<?>) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Box [value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
