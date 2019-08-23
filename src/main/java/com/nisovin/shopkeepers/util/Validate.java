package com.nisovin.shopkeepers.util;

public class Validate {

	private Validate() {
	}

	// ARGUMENTS

	public static void error(String errorMessage) {
		throw new IllegalArgumentException(errorMessage);
	}

	public static <T> T notNull(T object) {
		return notNull(object, "The validated object is null");
	}

	public static <T> T notNull(T object, String errorMessage) {
		if (object == null) {
			error(errorMessage);
		}
		return object;
	}

	public static String notEmpty(String string) {
		return notEmpty(string, "The validated string is null or empty");
	}

	public static String notEmpty(String string, String errorMessage) {
		if (string == null || string.isEmpty()) {
			error(errorMessage);
		}
		return string;
	}

	public static void isTrue(boolean expression) {
		isTrue(expression, "The validated expression is false");
	}

	public static void isTrue(boolean expression, String errorMessage) {
		if (!expression) {
			error(errorMessage);
		}
	}

	public static double isFinite(double value) {
		return isFinite(value, "The validated double is infinite or NaN)");
	}

	public static double isFinite(double value, String errorMessage) {
		if (!Double.isFinite(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static double notNaN(double value) {
		return notNaN(value, "The validated double is NaN)");
	}

	public static double notNaN(double value, String errorMessage) {
		if (Double.isNaN(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static float isFinite(float value) {
		return isFinite(value, "The validated float is infinite or NaN)");
	}

	public static float isFinite(float value, String errorMessage) {
		if (!Float.isFinite(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static float notNaN(float value) {
		return notNaN(value, "The validated float is NaN)");
	}

	public static float notNaN(float value, String errorMessage) {
		if (Float.isNaN(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static void noNullElements(Iterable<?> iterable, String errorMessage) {
		notNull(iterable, errorMessage);
		for (Object object : iterable) {
			notNull(object, errorMessage);
		}
	}

	// STATE

	public static class State {

		private State() {
		}

		public static void error(String errorMessage) {
			throw new IllegalStateException(errorMessage);
		}

		public static <T> T notNull(T object) {
			return notNull(object, "The validated object is null");
		}

		public static <T> T notNull(T object, String errorMessage) {
			if (object == null) {
				error(errorMessage);
			}
			return object;
		}

		public static String notEmpty(String string) {
			return notEmpty(string, "The validated string is null or empty");
		}

		public static String notEmpty(String string, String errorMessage) {
			if (string == null || string.isEmpty()) {
				error(errorMessage);
			}
			return string;
		}

		public static void isTrue(boolean expression) {
			isTrue(expression, "The validated expression is false");
		}

		public static void isTrue(boolean expression, String errorMessage) {
			if (!expression) {
				error(errorMessage);
			}
		}

		public static double isFinite(double value) {
			return isFinite(value, "The validated double is infinite or NaN)");
		}

		public static double isFinite(double value, String errorMessage) {
			if (!Double.isFinite(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static double notNaN(double value) {
			return notNaN(value, "The validated double is NaN)");
		}

		public static double notNaN(double value, String errorMessage) {
			if (Double.isNaN(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static float isFinite(float value) {
			return isFinite(value, "The validated float is infinite or NaN)");
		}

		public static float isFinite(float value, String errorMessage) {
			if (!Float.isFinite(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static float notNaN(float value) {
			return notNaN(value, "The validated float is NaN)");
		}

		public static float notNaN(float value, String errorMessage) {
			if (Float.isNaN(value)) {
				error(errorMessage);
			}
			return value;
		}
	}
}
