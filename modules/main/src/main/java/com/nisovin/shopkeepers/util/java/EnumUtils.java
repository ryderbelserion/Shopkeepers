package com.nisovin.shopkeepers.util.java;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class EnumUtils {

	public static <T extends Enum<T>> @NonNull T cycleEnumConstant(
			Class<? extends @NonNull T> enumClass,
			@NonNull T current,
			boolean backwards
	) {
		return cycleEnumConstant(enumClass, current, backwards, PredicateUtils.alwaysTrue());
	}

	public static <T extends Enum<T>> @NonNull T cycleEnumConstant(
			Class<? extends @NonNull T> enumClass,
			@NonNull T current,
			boolean backwards,
			Predicate<? super @NonNull T> predicate
	) {
		return EnumUtils.<@NonNull T, @NonNull T>cycleEnumConstant(
				enumClass,
				false,
				current,
				backwards,
				predicate
		);
	}

	public static <T extends Enum<T>> @Nullable T cycleEnumConstantNullable(
			Class<? extends @NonNull T> enumClass,
			@Nullable T current,
			boolean backwards
	) {
		return cycleEnumConstantNullable(
				enumClass,
				current,
				backwards,
				PredicateUtils.<@Nullable T>alwaysTrue()
		);
	}

	public static <T extends Enum<T>> @Nullable T cycleEnumConstantNullable(
			Class<? extends @NonNull T> enumClass,
			@Nullable T current,
			boolean backwards,
			Predicate<? super @Nullable T> predicate
	) {
		return EnumUtils.<@NonNull T, @Nullable T>cycleEnumConstant(
				enumClass,
				true,
				current,
				backwards,
				predicate
		);
	}

	// nullable: Uses null as first value.
	// current==null: nullable has to be true.
	// Cycled through all values but none got accepted: Returns current value (can be null).
	private static <E extends Enum<E>, T extends @Nullable E> T cycleEnumConstant(
			Class<? extends @NonNull T> enumClass,
			boolean nullable,
			T current,
			boolean backwards,
			Predicate<? super T> predicate
	) {
		Validate.notNull(enumClass, "enumClass is null");
		@NonNull T[] values = Unsafe.assertNonNull(enumClass.getEnumConstants());
		List<@NonNull T> valuesList = Arrays.asList(values);
		return CollectionUtils.cycleValue(valuesList, nullable, current, backwards, predicate);
	}

	public static <E extends Enum<E>> @Nullable E valueOf(
			Class<E> enumType,
			@Nullable String enumName
	) {
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
	 * This converts the enum name to lower case, replaces underscores with spaces, and capitalizes
	 * the leading characters of all words.
	 * 
	 * @param enumName
	 *            the enum name, not <code>null</code>
	 * @return the formatted enum name
	 */
	public static String formatEnumName(String enumName) {
		Validate.notNull(enumName, "enumName is null");
		String formatted = enumName;
		formatted = formatted.toLowerCase(Locale.ROOT);
		formatted = formatted.replace('_', ' ');
		formatted = StringUtils.capitalizeAll(formatted);
		return formatted;
	}

	/**
	 * Formats the given String in a way that is typical for enum names.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace, dashes, and
	 * dots to underscores ('{@code _}'), and converts all characters to upper case.
	 * 
	 * @param enumName
	 *            the enum name to normalize, not <code>null</code>
	 * @return the normalized enum name
	 */
	public static String normalizeEnumName(String enumName) {
		Validate.notNull(enumName, "enumName is null");
		String normalized = enumName;
		normalized = normalized.trim();
		normalized = normalized.replace('-', '_');
		normalized = normalized.replace('.', '_');
		normalized = StringUtils.replaceWhitespace(normalized, "_");
		normalized = normalized.toUpperCase(Locale.ROOT);
		return normalized;
	}

	private EnumUtils() {
	}
}
