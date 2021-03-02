package com.nisovin.shopkeepers.util;

import java.util.Locale;

import org.bukkit.NamespacedKey;

public class NamespacedKeyUtils {

	public static final char NAMESPACED_KEY_SEPARATOR_CHAR = ':';
	public static final String NAMESPACED_KEY_SEPARATOR = String.valueOf(NAMESPACED_KEY_SEPARATOR_CHAR);

	public static final String MINECRAFT_NAMESPACE_PREFIX = NamespacedKey.MINECRAFT + NAMESPACED_KEY_SEPARATOR;

	/**
	 * Parses a {@link NamespacedKey} from the given input String.
	 * <p>
	 * The given input String is formatted in a way that matches valid {@link NamespacedKey NamespacedKeys}: Leading and
	 * trailing whitespace is removed. All remaining whitespace is converted to underscores, since this is what
	 * Minecraft's namespaced keys use. All characters are converted to lower case.
	 * <p>
	 * If no namespace is specified, this uses {@link NamespacedKey#MINECRAFT} as namespace.
	 * <p>
	 * If the used namespace (either specified or due to default) is {@link NamespacedKey#MINECRAFT}, dashes in the key
	 * are converted to underscores as well, because this is what Minecraft uses for its keys. For any other namespace,
	 * they are kept as is, because they may be valid keys.
	 *
	 * @param input
	 *            the input String
	 * @return the parsed {@link NamespacedKey}, or <code>null</code> if the input is invalid
	 */
	public static NamespacedKey parse(String input) {
		if (input == null || input.isEmpty()) return null;

		// General formatting:
		input = input.trim();
		input = StringUtils.replaceWhitespace(input, "_");
		input = input.toLowerCase(Locale.ROOT);

		// Split at separator:
		String[] components = input.split(NAMESPACED_KEY_SEPARATOR, 3);
		// We expect at most two components (namespace and key):
		if (components.length > 2) {
			return null;
		}

		// Retrieve the (optional) namespace and the key:
		String namespace;
		String key;
		if (components.length == 2) {
			namespace = components[0];
			key = components[1];
		} else {
			assert components.length == 1;
			namespace = ""; // No namespace specified
			key = components[0];
		}
		assert namespace != null && key != null;
		// However, both the namespace and the key may still be empty.

		// If no namespace is specified, fallback to Minecraft's namespace:
		if (namespace.isEmpty()) {
			namespace = NamespacedKey.MINECRAFT;
		}

		// Basic key validation:
		if (key.isEmpty()) {
			return null;
		}
		assert !namespace.isEmpty() && !key.isEmpty();

		// We know that Minecraft's namespaced keys use underscores, so we can convert dashes to underscores (allows for
		// more lenient parsing):
		if (namespace.equals(NamespacedKey.MINECRAFT)) {
			key = key.replace("-", "_");
		}

		// Create the NamespacedKey:
		// This performs additional validation internally and throws an IllegalArgumentException if the namespaced key
		// is invalid.
		NamespacedKey namespacedKey;
		try {
			namespacedKey = new NamespacedKey(namespace, key);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return namespacedKey;
	}

	private NamespacedKeyUtils() {
	}
}
