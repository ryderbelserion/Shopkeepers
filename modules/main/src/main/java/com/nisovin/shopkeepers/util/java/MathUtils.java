package com.nisovin.shopkeepers.util.java;

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

	/**
	 * Gets the sum of the given integers, clamped to {@link Integer#MAX_VALUE} and {@link Integer#MIN_VALUE} if an
	 * overflow would occur.
	 * 
	 * @param x
	 *            the first value
	 * @param y
	 *            the second value
	 * @return the result
	 */
	public static int addSaturated(int x, int y) {
		int result = x + y;
		if (((x ^ result) & (y ^ result)) < 0) {
			if (result < 0) {
				// Overflow occurred:
				return Integer.MAX_VALUE;
			} else {
				// Underflow occurred:
				return Integer.MIN_VALUE;
			}
		}
		return result;
	}

	/**
	 * Calculates the closest value in the specified range.
	 * 
	 * @param value
	 *            the value
	 * @param min
	 *            the lower bound (inclusive)
	 * @param max
	 *            the upper bound (inclusive)
	 * @return the value within the specified range
	 */
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
	 * <p>
	 * If no values are given, {@code 0} is returned.
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
	 * Calculates the average of the given values, ignoring values that match the specified one.
	 * <p>
	 * The average is calculated by forming the sum of all values and then dividing by the number of values. If the sum
	 * of the given values does not fit into a single <code>long</code>, it can overflow and produce an incorrect
	 * result.
	 * <p>
	 * If no values are given, {@code 0} is returned.
	 * 
	 * @param values
	 *            the values
	 * @param ignore
	 *            the value to ignore
	 * @return the average
	 */
	public static double average(long[] values, long ignore) {
		long total = 0L;
		int ignored = 0;
		for (long value : values) {
			if (value == ignore) {
				ignored += 1;
				continue;
			}
			total += value;
		}
		int elementCount = values.length - ignored;
		if (elementCount == 0) {
			return 0L;
		} else {
			return ((double) total / elementCount);
		}
	}

	/**
	 * Calculates the maximum of the given values.
	 * <p>
	 * If no values are given, {@code 0} is returned.
	 * 
	 * @param values
	 *            the values
	 * @return the max value
	 */
	public static long max(long[] values) {
		if (values.length == 0) return 0L;
		long max = Long.MIN_VALUE;
		for (long value : values) {
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	/**
	 * Calculates the maximum of the given values, ignoring values that match the specified one.
	 * <p>
	 * If no values are given, {@code 0} is returned.
	 * 
	 * @param values
	 *            the values
	 * @param ignore
	 *            the value to ignore
	 * @return the max value
	 */
	public static long max(long[] values, long ignore) {
		long max = Long.MIN_VALUE;
		int ignored = 0;
		for (long value : values) {
			if (value == ignore) {
				ignored += 1;
				continue;
			}
			if (value > max) {
				max = value;
			}
		}
		if (values.length - ignored == 0) return 0L;
		return max;
	}

	/**
	 * Brings the given value into the specified range via a modulo (cyclic) operation.
	 * 
	 * @param value
	 *            the value
	 * @param min
	 *            the lower bound (inclusive)
	 * @param max
	 *            the upper bound (inclusive)
	 * @return the value within the specified range
	 */
	public static int rangeModulo(int value, int min, int max) {
		Validate.isTrue(min <= max, "min > max");
		// Note: value can be outside of this range.
		int offset = min;
		int range = max - min + 1;
		int modulo = (value - offset) % range;
		if (modulo < 0) modulo += range;
		return offset + modulo;
	}
}
