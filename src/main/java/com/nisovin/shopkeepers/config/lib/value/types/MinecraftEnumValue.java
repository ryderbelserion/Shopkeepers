package com.nisovin.shopkeepers.config.lib.value.types;

import org.bukkit.NamespacedKey;

/**
 * Extends {@link EnumValue} and removes Minecraft's namespace prefix (i.e. {@link NamespacedKey#MINECRAFT}), if it is
 * present, when parsing the enum value from a given String input.
 *
 * @param <E>
 *            the enum type
 */
public class MinecraftEnumValue<E extends Enum<E>> extends EnumValue<E> {

	// Upper case because we normalize inputs to match the names of enum constants, which are upper case:
	private static final String MINECRAFT_NAMESPACE_PREFIX = NamespacedKey.MINECRAFT + ":";

	public MinecraftEnumValue(Class<E> enumClass) {
		super(enumClass);
	}

	@Override
	protected String normalize(String input) {
		assert input != null;
		String normalized = super.normalize(input);
		// Remove the Minecraft namespace prefix, if it is present:
		// Note: Any other namespace prefix is not supported in will result in a failed lookup of the enum value.
		if (normalized.startsWith(MINECRAFT_NAMESPACE_PREFIX)) {
			normalized = normalized.substring(MINECRAFT_NAMESPACE_PREFIX.length());
		}
		return normalized;
	}
}
