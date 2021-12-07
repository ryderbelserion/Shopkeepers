package com.nisovin.shopkeepers.util.java;

import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Validate {

	// ARGUMENTS

	public static void error(String errorMessage) {
		throw new IllegalArgumentException(errorMessage);
	}

	// Note: Throws a NPE if the supplier is null, similar to how Logger throws a NPE.
	public static void error(Supplier<String> errorMessageSupplier) {
		throw new IllegalArgumentException(errorMessageSupplier.get());
	}

	public static <T> T notNull(T object) {
		return notNull(object, "object is null");
	}

	public static <T> T notNull(T object, String errorMessage) {
		if (object == null) {
			error(errorMessage);
		}
		return object;
	}

	public static <T> T notNull(T object, Supplier<String> errorMessageSupplier) {
		if (object == null) {
			error(errorMessageSupplier);
		}
		return object;
	}

	public static String notEmpty(String string) {
		return notEmpty(string, "string is null or empty");
	}

	public static String notEmpty(String string, String errorMessage) {
		if (string == null || string.isEmpty()) {
			error(errorMessage);
		}
		return string;
	}

	public static String notEmpty(String string, Supplier<String> errorMessageSupplier) {
		if (string == null || string.isEmpty()) {
			error(errorMessageSupplier);
		}
		return string;
	}

	public static boolean isTrue(boolean expression) {
		return isTrue(expression, "expression evaluates to false");
	}

	public static boolean isTrue(boolean expression, String errorMessage) {
		if (!expression) {
			error(errorMessage);
		}
		return expression;
	}

	public static boolean isTrue(boolean expression, Supplier<String> errorMessageSupplier) {
		if (!expression) {
			error(errorMessageSupplier);
		}
		return expression;
	}

	public static <T> T isTrue(T value, Predicate<T> predicate) {
		return isTrue(value, predicate, "predicate evaluates to false");
	}

	public static <T> T isTrue(T value, Predicate<T> predicate, String errorMessage) {
		if (!predicate.test(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static <T> T isTrue(T value, Predicate<T> predicate, Supplier<String> errorMessageSupplier) {
		if (!predicate.test(value)) {
			error(errorMessageSupplier);
		}
		return value;
	}

	public static double isFinite(double value) {
		return isFinite(value, "value is not finite");
	}

	public static double isFinite(double value, String errorMessage) {
		if (!Double.isFinite(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static double isFinite(double value, Supplier<String> errorMessageSupplier) {
		if (!Double.isFinite(value)) {
			error(errorMessageSupplier);
		}
		return value;
	}

	public static double notNaN(double value) {
		return notNaN(value, "value is NaN");
	}

	public static double notNaN(double value, String errorMessage) {
		if (Double.isNaN(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static double notNaN(double value, Supplier<String> errorMessageSupplier) {
		if (Double.isNaN(value)) {
			error(errorMessageSupplier);
		}
		return value;
	}

	public static float isFinite(float value) {
		return isFinite(value, "value is not finite");
	}

	public static float isFinite(float value, String errorMessage) {
		if (!Float.isFinite(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static float isFinite(float value, Supplier<String> errorMessageSupplier) {
		if (!Float.isFinite(value)) {
			error(errorMessageSupplier);
		}
		return value;
	}

	public static float notNaN(float value) {
		return notNaN(value, "value is NaN");
	}

	public static float notNaN(float value, String errorMessage) {
		if (Float.isNaN(value)) {
			error(errorMessage);
		}
		return value;
	}

	public static float notNaN(float value, Supplier<String> errorMessageSupplier) {
		if (Float.isNaN(value)) {
			error(errorMessageSupplier);
		}
		return value;
	}

	public static <T extends Iterable<?>> T noNullElements(T iterable, String errorMessage) {
		notNull(iterable, errorMessage);
		for (Object object : iterable) {
			notNull(object, errorMessage);
		}
		return iterable;
	}

	public static <T extends Iterable<?>> T noNullElements(T iterable, Supplier<String> errorMessageSupplier) {
		notNull(iterable, errorMessageSupplier);
		for (Object object : iterable) {
			notNull(object, errorMessageSupplier);
		}
		return iterable;
	}

	// STATE

	public static final class State {

		public static void error(String errorMessage) {
			throw new IllegalStateException(errorMessage);
		}

		// Note: Throws a NPE if the supplier is null, similar to how Logger throws a NPE.
		public static void error(Supplier<String> errorMessageSupplier) {
			throw new IllegalStateException(errorMessageSupplier.get());
		}

		public static <T> T notNull(T object) {
			return notNull(object, "object is null");
		}

		public static <T> T notNull(T object, String errorMessage) {
			if (object == null) {
				error(errorMessage);
			}
			return object;
		}

		public static <T> T notNull(T object, Supplier<String> errorMessageSupplier) {
			if (object == null) {
				error(errorMessageSupplier);
			}
			return object;
		}

		public static String notEmpty(String string) {
			return notEmpty(string, "string is null or empty");
		}

		public static String notEmpty(String string, String errorMessage) {
			if (string == null || string.isEmpty()) {
				error(errorMessage);
			}
			return string;
		}

		public static String notEmpty(String string, Supplier<String> errorMessageSupplier) {
			if (string == null || string.isEmpty()) {
				error(errorMessageSupplier);
			}
			return string;
		}

		public static boolean isTrue(boolean expression) {
			return isTrue(expression, "expression evaluates to false");
		}

		public static boolean isTrue(boolean expression, String errorMessage) {
			if (!expression) {
				error(errorMessage);
			}
			return expression;
		}

		public static boolean isTrue(boolean expression, Supplier<String> errorMessageSupplier) {
			if (!expression) {
				error(errorMessageSupplier);
			}
			return expression;
		}

		public static <T> T isTrue(T value, Predicate<T> predicate) {
			return isTrue(value, predicate, "predicate evaluates to false");
		}

		public static <T> T isTrue(T value, Predicate<T> predicate, String errorMessage) {
			if (!predicate.test(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static <T> T isTrue(T value, Predicate<T> predicate, Supplier<String> errorMessageSupplier) {
			if (!predicate.test(value)) {
				error(errorMessageSupplier);
			}
			return value;
		}

		public static double isFinite(double value) {
			return isFinite(value, "value is not finite");
		}

		public static double isFinite(double value, String errorMessage) {
			if (!Double.isFinite(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static double isFinite(double value, Supplier<String> errorMessageSupplier) {
			if (!Double.isFinite(value)) {
				error(errorMessageSupplier);
			}
			return value;
		}

		public static double notNaN(double value) {
			return notNaN(value, "value is NaN");
		}

		public static double notNaN(double value, String errorMessage) {
			if (Double.isNaN(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static double notNaN(double value, Supplier<String> errorMessageSupplier) {
			if (Double.isNaN(value)) {
				error(errorMessageSupplier);
			}
			return value;
		}

		public static float isFinite(float value) {
			return isFinite(value, "value is not finite");
		}

		public static float isFinite(float value, String errorMessage) {
			if (!Float.isFinite(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static float isFinite(float value, Supplier<String> errorMessageSupplier) {
			if (!Float.isFinite(value)) {
				error(errorMessageSupplier);
			}
			return value;
		}

		public static float notNaN(float value) {
			return notNaN(value, "value is NaN");
		}

		public static float notNaN(float value, String errorMessage) {
			if (Float.isNaN(value)) {
				error(errorMessage);
			}
			return value;
		}

		public static float notNaN(float value, Supplier<String> errorMessageSupplier) {
			if (Float.isNaN(value)) {
				error(errorMessageSupplier);
			}
			return value;
		}

		public static <T extends Iterable<?>> T noNullElements(T iterable, String errorMessage) {
			notNull(iterable, errorMessage);
			for (Object object : iterable) {
				notNull(object, errorMessage);
			}
			return iterable;
		}

		public static <T extends Iterable<?>> T noNullElements(T iterable, Supplier<String> errorMessageSupplier) {
			notNull(iterable, errorMessageSupplier);
			for (Object object : iterable) {
				notNull(object, errorMessageSupplier);
			}
			return iterable;
		}

		private State() {
		}
	}

	private Validate() {
	}
}
