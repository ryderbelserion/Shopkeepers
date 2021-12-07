package com.nisovin.shopkeepers.util.java;

import java.util.Locale;
import java.util.function.Predicate;

public final class EnumUtils {

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

	// nullable: Uses null as first value.
	// current==null: nullable has to be true.
	// Cycled through all values but none got accepted: Returns current value (can be null).
	private static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, boolean nullable, T current, boolean backwards, Predicate<T> predicate) {
		Validate.notNull(enumClass, "enumClass is null");
		Validate.isTrue(current != null || nullable, "Not nullable, but current is null");
		predicate = PredicateUtils.orAlwaysTrue(predicate);
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
			if (predicate.test(next)) {
				return next;
			}
		}
	}

	public static <E extends Enum<E>> E valueOf(Class<E> enumType, String enumName) {
		if (enumName == null) return null;
		try {
			return Enum.valueOf(enumType, enumName);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Formats the given enum name to a more user-friendly representation.
	 * <p>
	 * This converts the enum name to lower case, replaces underscores with spaces, and capitalizes the leading
	 * characters of all words.
	 * <p>
	 * This returns an empty String if the given enum name is <code>null</code>.
	 * 
	 * @param enumName
	 *            the enum name
	 * @return the formatted enum name
	 */
	public static String formatEnumName(String enumName) {
		if (enumName == null) enumName = "";
		enumName = enumName.toLowerCase(Locale.ROOT);
		enumName = enumName.replace('_', ' ');
		enumName = StringUtils.capitalizeAll(enumName);
		return enumName;
	}

	/**
	 * Formats the given String in a way that is typical for enum names.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace, dashes, and dots to underscores
	 * ('{@code _}'), and converts all characters to upper case.
	 * <p>
	 * This returns <code>null</code> if the input String is <code>null</code>.
	 * 
	 * @param enumName
	 *            the enum name to normalize
	 * @return the normalized enum name
	 */
	public static String normalizeEnumName(String enumName) {
		if (enumName == null) return null;
		enumName = enumName.trim();
		enumName = enumName.replace('-', '_');
		enumName = enumName.replace('.', '_');
		enumName = StringUtils.replaceWhitespace(enumName, "_");
		enumName = enumName.toUpperCase(Locale.ROOT);
		return enumName;
	}

	private EnumUtils() {
	}
}
