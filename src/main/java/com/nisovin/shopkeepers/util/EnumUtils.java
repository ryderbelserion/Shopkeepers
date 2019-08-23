package com.nisovin.shopkeepers.util;

import java.util.function.Predicate;

public class EnumUtils {

	private EnumUtils() {
	}

	public static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, T current, boolean backwards) {
		return cycleEnumConstant(enumClass, current, backwards, null);
	}

	public static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, T current, boolean backwards, Predicate<T> predicate) {
		return cycleEnumConstant(enumClass, false, current, backwards, predicate);
	}

	public static <T extends Enum<T>> T cycleEnumConstantNullable(Class<T> enumClass, T current, boolean backwards) {
		return cycleEnumConstantNullable(enumClass, current, backwards, null);
	}

	public static <T extends Enum<T>> T cycleEnumConstantNullable(Class<T> enumClass, T current, boolean backwards, Predicate<T> predicate) {
		return cycleEnumConstant(enumClass, true, current, backwards, predicate);
	}

	// nullable: uses null as first value
	// current==null: nullable has to be true
	// cycled through all values but none got accepted: returns current value (can be null)
	private static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, boolean nullable, T current, boolean backwards, Predicate<T> predicate) {
		Validate.notNull(enumClass);
		Validate.isTrue(current != null || nullable, "Not nullable, but current is null!");
		T[] values = enumClass.getEnumConstants();
		int currentId = (current == null ? -1 : current.ordinal());
		int nextId = currentId;
		while (true) {
			if (backwards) {
				nextId -= 1;
				if (nextId < (nullable ? -1 : 0)) {
					nextId = (values.length - 1);
				}
			} else {
				nextId += 1;
				if (nextId >= values.length) {
					nextId = (nullable ? -1 : 0);
				}
			}
			if (nextId == currentId) {
				return current;
			}
			T next = (nextId == -1 ? null : values[nextId]);
			if (predicate == null || predicate.test(next)) {
				return next;
			}
		}
	}

	public static <E extends Enum<E>> E parseEnumValue(Class<E> enumClass, String name) {
		if (name == null) return null;
		try {
			return Enum.valueOf(enumClass, name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
