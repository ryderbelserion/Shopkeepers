package com.nisovin.shopkeepers.util;

import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {

	private MathUtils() {
	}

	/**
	 * Gets a random integer between the given min value (inclusive) and max value (exclusive).
	 * 
	 * @param min
	 *            the minimum value (inclusive)
	 * @param max
	 *            the maximum value (exclusive)
	 * @return the random value in between
	 */
	public static int randomInRange(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max);
	}

	public static int trim(int value, int min, int max) {
		if (value <= min) return min;
		if (value >= max) return max;
		return value;
	}

	/**
	 * Calculates the average of the given values.
	 * <p>
	 * The average is calculated by forming the sum of all values and then dividing by the number of values. If the sum
	 * of the given values does not fit into a single <code>long</code>, it can overflow and produce an incorrect
	 * result.
	 * 
	 * @param values
	 *            the values
	 * @return the average
	 */
	public static double average(long[] values) {
		long total = 0L;
		for (long value : values) {
			total += value;
		}
		return ((double) total / values.length);
	}

	/**
	 * Calculates the maximum of the given values.
	 * <p>
	 * Returns {@link Long#MIN_VALUE} if no values are given.
	 * 
	 * @param values
	 *            the values
	 * @return the max value
	 */
	public static long max(long[] values) {
		long max = Long.MIN_VALUE;
		for (long value : values) {
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	// Brings the given value into the specified range via a modulo operation.
	public static int rangeModulo(int value, int min, int max) {
		assert min <= max; // Note: value can be outside the range.
		int offset = min;
		int range = max - min + 1;
		int modulo = (value - offset) % range;
		if (modulo < 0) modulo += range;
		return offset + modulo;
	}
}
