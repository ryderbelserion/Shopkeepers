package com.nisovin.shopkeepers.util;

/**
 * A counter that resets to {@code 0} whenever its value reaches a specified upper bound (exclusive).
 * <p>
 * Not thread-safe.
 */
public class CyclicCounter {

	private final int upperBound; // Exclusive
	private int value = 0;

	/**
	 * Creates a new {@link CyclicCounter}.
	 * 
	 * @param upperBound
	 *            the upper bound (exclusive), has to be positive
	 */
	public CyclicCounter(int upperBound) {
		Validate.isTrue(upperBound > 0, "upperBound has to be positive");
		this.upperBound = upperBound;
	}

	/**
	 * Gets the current value, without incrementing it.
	 * 
	 * @return the current value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Resets this counter back to {@code 0}.
	 */
	public void reset() {
		value = 0;
	}

	/**
	 * Gets the current value and then increments it by one.
	 * <p>
	 * If the new value reaches the upper bound of this counter, it is reset to {@link 0}.
	 * 
	 * @return the current value (prior to the increment)
	 */
	public int getAndIncrement() {
		int currentValue = value;
		int nextValue = currentValue + 1;
		if (nextValue >= upperBound) {
			value = 0;
		} else {
			value = nextValue;
		}
		return currentValue;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CyclicCounter [upperBound=");
		builder.append(upperBound);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
