package com.nisovin.shopkeepers.util.bukkit;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class MinecraftEnumUtils {

	// Upper case because we normalize Strings to match the names of enum constants, which are upper
	// case, before we check for this prefix:
	private static final String MINECRAFT_NAMESPACE_PREFIX = NamespacedKeyUtils.MINECRAFT_NAMESPACE_PREFIX
			.toUpperCase(Locale.ROOT);

	/**
	 * Formats the given String in a way that matches the names of the Bukkit enum values.
	 * <p>
	 * This performs the same transformations as {@link EnumUtils#normalizeEnumName(String)}, but
	 * additionally strips Minecraft's namespace prefix (i.e. {@link NamespacedKey#MINECRAFT}), if
	 * it is present.
	 * 
	 * @param enumName
	 *            the enum name, not <code>null</code>
	 * @return the formatted enum name
	 */
	public static String normalizeEnumName(String enumName) {
		Validate.notNull(enumName, "enumName is null");
		String normalized = EnumUtils.normalizeEnumName(enumName);

		// Remove the Minecraft namespace prefix, if it is present:
		if (normalized.startsWith(MINECRAFT_NAMESPACE_PREFIX)) {
			normalized = normalized.substring(MINECRAFT_NAMESPACE_PREFIX.length());
		}
		return normalized;
	}

	/**
	 * Tries the parse an enum value from the given String input.
	 * <p>
	 * This first attempts to find an enum value with perfectly matching name, and otherwise tries
	 * to find a matching enum value by {@link #normalizeEnumName(String) formatting} the given enum
	 * name so that it matches the usual formatting of Bukkit enums.
	 * 
	 * @param <E>
	 *            the enum type
	 * @param enumType
	 *            the enum class, not <code>null</code>
	 * @param enumName
	 *            the enum value name to parse, can be <code>null</code>
	 * @return the parsed enum value, or <code>null</code> if the enum value cannot be parsed
	 */
	public static <E extends Enum<E>> @Nullable E parseEnum(
			Class<E> enumType,
			@Nullable String enumName
	) {
		Validate.notNull(enumType, "enumType is null");
		if (enumName == null || enumName.isEmpty()) return null;

		// Try to parse the enum value without normalizing the enum name first (in case the enum
		// does not adhere to the expected normalized format):
		@Nullable E enumValue = EnumUtils.valueOf(enumType, enumName);
		if (enumValue != null) return enumValue;

		// Try with enum name normalization:
		String normalizedEnumName = normalizeEnumName(enumName);
		return EnumUtils.valueOf(enumType, normalizedEnumName); // Returns null if parsing fails
	}

	private MinecraftEnumUtils() {
	}
}
