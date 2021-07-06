package com.nisovin.shopkeepers.util.bukkit;

import java.util.Locale;

import org.bukkit.NamespacedKey;

import com.nisovin.shopkeepers.util.java.EnumUtils;

public class MinecraftEnumUtils {

	// Upper case because we normalize Strings to match the names of enum constants, which are upper case, before we
	// check for this prefix:
	private static final String MINECRAFT_NAMESPACE_PREFIX = NamespacedKeyUtils.MINECRAFT_NAMESPACE_PREFIX.toUpperCase(Locale.ROOT);

	/**
	 * Formats the given String in a way that matches the names of the Bukkit enum values.
	 * <p>
	 * This performs the same transformations as {@link EnumUtils#normalizeEnumName(String)}, but additionally strips
	 * Minecraft's namespace prefix (i.e. {@link NamespacedKey#MINECRAFT}), if it is present.
	 * <p>
	 * This returns <code>null</code> if the given input String is <code>null</code>.
	 * 
	 * @param enumName
	 *            the enum name
	 * @return the formatted enum name
	 */
	public static String normalizeEnumName(String enumName) {
		if (enumName == null) return null;
		enumName = EnumUtils.normalizeEnumName(enumName);

		// Remove the Minecraft namespace prefix, if it is present:
		if (enumName.startsWith(MINECRAFT_NAMESPACE_PREFIX)) {
			enumName = enumName.substring(MINECRAFT_NAMESPACE_PREFIX.length());
		}
		return enumName;
	}

	private MinecraftEnumUtils() {
	}
}
