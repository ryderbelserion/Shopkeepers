package com.nisovin.shopkeepers.util;

public class MathUtils {

	private MathUtils() {
	}

	public static int trim(int value, int min, int max) {
		if (value <= min) return min;
		if (value >= max) return max;
		return value;
	}

	/**
	 * Calculates the average of the given values.
	 * <p>
	 * Note: This can overflow if the sum of the values doesn't fit into a single <code>long</code>.
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
}
