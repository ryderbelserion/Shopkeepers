package com.nisovin.shopkeepers.util.bukkit;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class NamespacedKeyUtils {

	public static final char NAMESPACED_KEY_SEPARATOR_CHAR = ':';
	public static final String NAMESPACED_KEY_SEPARATOR = String.valueOf(NAMESPACED_KEY_SEPARATOR_CHAR);

	public static final String MINECRAFT_NAMESPACE_PREFIX = NamespacedKey.MINECRAFT + NAMESPACED_KEY_SEPARATOR;

	/**
	 * Creates a new {@link NamespacedKey}.
	 * 
	 * @param namespace
	 *            the namespace
	 * @param key
	 *            the key
	 * @return the namespaced key
	 * @throws IllegalArgumentException
	 *             if the namespace or key are invalid
	 */
	public static NamespacedKey create(String namespace, String key) {
		return new NamespacedKey(namespace, key);
	}

	/**
	 * Parses a {@link NamespacedKey} from the given input String.
	 * <p>
	 * The given input String is formatted in a way that matches valid {@link NamespacedKey
	 * NamespacedKeys} (see {@link #normalizeNamespacedKey(String)}).
	 * <p>
	 * If no namespace is specified, this uses {@link NamespacedKey#MINECRAFT} as namespace.
	 * <p>
	 * If the namespace is {@link NamespacedKey#MINECRAFT} (either specified or due to default),
	 * dashes in the key are converted to underscores, because this is what Minecraft uses for its
	 * keys (see {@link #normalizeMinecraftNamespacedKey(String)}). For any other namespace, they
	 * are kept as is, because they might be valid keys.
	 *
	 * @param input
	 *            the input String
	 * @return the parsed {@link NamespacedKey}, or <code>null</code> if the input is invalid
	 */
	public static @Nullable NamespacedKey parse(String input) {
		Validate.notNull(input, "input is null");
		if (input.isEmpty()) return null;

		// General formatting:
		String normalizedInput = normalizeNamespacedKey(input);

		// Split at separator:
		String[] components = normalizedInput.split(NAMESPACED_KEY_SEPARATOR, 3);
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

		// We know that Minecraft's namespaced keys use underscores, so we can convert dashes to
		// underscores (allows for more lenient parsing):
		if (namespace.equals(NamespacedKey.MINECRAFT)) {
			key = key.replace('-', '_');
		}

		// Create the NamespacedKey:
		// This performs additional validation internally and throws an IllegalArgumentException if
		// the namespaced key is invalid.
		NamespacedKey namespacedKey;
		try {
			namespacedKey = NamespacedKeyUtils.create(namespace, key);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return namespacedKey;
	}

	/**
	 * Formats the given input String in a way that matches valid {@link NamespacedKey
	 * NamespacedKeys}.
	 * <p>
	 * Leading and trailing whitespace is removed. All remaining whitespace is converted to
	 * underscores, since this is what Minecraft's namespaced keys use. All characters are converted
	 * to lower case.
	 * 
	 * @param namespacedKey
	 *            the input String, not <code>null</code>
	 * @return the formatted String, not <code>null</code>
	 */
	public static String normalizeNamespacedKey(String namespacedKey) {
		Validate.notNull(namespacedKey, "namespacedKey is null");
		String normalized = namespacedKey.trim();
		// Without further contextual knowledge, we don't know what kind of separator the namespaced
		// key is supposed to use. We normalize whitespace to underscores because this is what
		// Minecraft uses for its own keys:
		normalized = StringUtils.replaceWhitespace(normalized, "_");
		normalized = normalized.toLowerCase(Locale.ROOT);
		return normalized;
	}

	/**
	 * Formats the given input String in a way that matches valid {@link NamespacedKey
	 * NamespacedKeys} as they are used by Minecraft.
	 * <p>
	 * This formats the given String in the same way as {@link #normalizeNamespacedKey(String)}, but
	 * additionally replaces all dashes ('{@code -}') with underscores ('{@code _}').
	 * 
	 * @param namespacedKey
	 *            the input String, not <code>null</code>
	 * @return the formatted String, not <code>null</code>
	 */
	public static String normalizeMinecraftNamespacedKey(String namespacedKey) {
		Validate.notNull(namespacedKey, "namespacedKey is null");
		String normalized = normalizeNamespacedKey(namespacedKey);
		// Minecraft's namespaced keys use underscores:
		normalized = normalized.replace('-', '_');
		return normalized;
	}

	private NamespacedKeyUtils() {
	}
}
