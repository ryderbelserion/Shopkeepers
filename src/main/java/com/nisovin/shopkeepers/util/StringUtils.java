package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility functions related to Strings.
 */
public class StringUtils {

	private StringUtils() {
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static String normalize(String identifier) {
		if (identifier == null) return null;
		return identifier.trim().replace('_', '-').replace(' ', '-').toLowerCase(Locale.ROOT);
	}

	public static List<String> normalize(List<String> identifiers) {
		if (identifiers == null) return null;
		List<String> normalized = new ArrayList<>(identifiers.size());
		for (String identifier : identifiers) {
			normalized.add(normalize(identifier));
		}
		return normalized;
	}
}
