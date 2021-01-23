package com.nisovin.shopkeepers.util;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

	private TimeUtils() {
	}

	/**
	 * Converts a duration between the given {@link TimeUnit time units} while preserving double precision.
	 * 
	 * @param duration
	 *            the duration in the source time unit
	 * @param from
	 *            the source time unit
	 * @param to
	 *            the target time unit
	 * @return the duration in the target time unit
	 */
	public static double convert(double duration, TimeUnit from, TimeUnit to) {
		if (from == to) {
			return duration;
		}
		// Smaller ordinal indicates the smaller time unit:
		if (from.ordinal() < to.ordinal()) {
			return duration / from.convert(1, to);
		} else {
			return duration * to.convert(1, from);
		}
	}
}
