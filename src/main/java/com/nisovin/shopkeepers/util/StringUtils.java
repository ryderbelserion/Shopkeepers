package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility functions related to Strings.
 */
public class StringUtils {

	private StringUtils() {
	}

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static String normalizeKeepCase(String identifier) {
		if (identifier == null) return null;
		identifier = identifier.trim();
		identifier = identifier.replace('_', '-');
		identifier = replaceWhitespace(identifier, "-");
		return identifier;
	}

	public static String normalize(String identifier) {
		if (identifier == null) return null;
		identifier = normalizeKeepCase(identifier);
		return identifier.toLowerCase(Locale.ROOT);
	}

	public static List<String> normalize(List<String> identifiers) {
		if (identifiers == null) return null;
		List<String> normalized = new ArrayList<>(identifiers.size());
		for (String identifier : identifiers) {
			normalized.add(normalize(identifier));
		}
		return normalized;
	}

	/**
	 * Checks if the given strings contains whitespace characters.
	 * 
	 * @param string
	 *            the string
	 * @return <code>true</code> if the given string is not empty and contains at least one whitespace character
	 */
	public static boolean containsWhitespace(String string) {
		if (isEmpty(string)) {
			return false;
		}

		int length = string.length();
		for (int i = 0; i < length; i++) {
			if (Character.isWhitespace(string.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all whitespace characters from the given string.
	 * 
	 * @param source
	 *            the source string
	 * @return the source string itself if it is empty, otherwise the string without any whitespace characters
	 */
	public static String removeWhitespace(String source) {
		return replaceWhitespace(source, "");
	}

	/**
	 * Replaces all whitespace characters from the given string.
	 * 
	 * @param source
	 *            the source string
	 * @param replacement
	 *            the replacement string
	 * @return the source string itself if it is empty, otherwise the string with any whitespace characters replaced
	 */
	public static String replaceWhitespace(String source, String replacement) {
		if (isEmpty(source)) {
			return source;
		}
		if (replacement == null) {
			replacement = "";
		}
		return WHITESPACE_PATTERN.matcher(source).replaceAll(replacement);
	}
}
