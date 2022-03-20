package com.nisovin.shopkeepers.util.java;

/**
 * A counter that resets to a specified lower bound (inclusive) whenever its value reaches a
 * specified upper bound (exclusive).
 * <p>
 * Not thread-safe.
 */
public class CyclicCounter {

	private final int lowerBound; // Inclusive
	private final int upperBound; // Exclusive, > lowerBound
	private int value;

	/**
	 * Creates a new {@link CyclicCounter} with a lower bound and initial value of {@code 0}.
	 * 
	 * @param upperBound
	 *            the upper bound (exclusive), has to be positive
	 */
	public CyclicCounter(int upperBound) {
		this(0, upperBound);
	}

	/**
	 * Creates a new {@link CyclicCounter}.
	 * 
	 * @param lowerBound
	 *            the lower bound (inclusive) and the initial value
	 * @param upperBound
	 *            the upper bound (exclusive), has to be greater than the lower bound
	 */
	public CyclicCounter(int lowerBound, int upperBound) {
		Validate.isTrue(upperBound > lowerBound, "lowerBound <= upperBound");
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.value = lowerBound;
	}

	/**
	 * Gets the lower bound.
	 * 
	 * @return the lower bound
	 */
	public int getLowerBound() {
		return lowerBound;
	}

	/**
	 * Gets the upper bound.
	 * 
	 * @return the upper bound
	 */
	public int getUpperBound() {
		return upperBound;
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
	 * Sets the current value.
	 * 
	 * @param value
	 *            the new value, has to be within the lower bound (inclusive) and the upper bound
	 *            (exclusive)
	 */
	public void setValue(int value) {
		Validate.isTrue(value >= lowerBound && value < upperBound, "value is out of bounds");
		this.value = value;
	}

	/**
	 * Resets this counter back to the {@code lower bound}.
	 */
	public void reset() {
		value = lowerBound;
	}

	/**
	 * Gets the current value and then increments it by one.
	 * <p>
	 * If the new value reaches the {@link #getUpperBound() upper bound} of this counter, the value
	 * is reset to the {@link #getLowerBound() lower bound}.
	 * 
	 * @return the current value prior to the increment
	 */
	public int getAndIncrement() {
		int currentValue = value;
		int nextValue = currentValue + 1;
		assert nextValue <= upperBound;
		if (nextValue == upperBound) {
			value = lowerBound;
		} else {
			value = nextValue;
		}
		return currentValue;
	}

	/**
	 * Increments the current value by one and then returns the new value.
	 * <p>
	 * If the new value reaches the {@link #getUpperBound() upper bound} of this counter, the value
	 * is reset to the {@link #getLowerBound() lower bound}.
	 * 
	 * @return the new value after the increment
	 */
	public int incrementAndGet() {
		this.getAndIncrement();
		return this.getValue();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CyclicCounter [lowerBound=");
		builder.append(lowerBound);
		builder.append(", upperBound=");
		builder.append(upperBound);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
