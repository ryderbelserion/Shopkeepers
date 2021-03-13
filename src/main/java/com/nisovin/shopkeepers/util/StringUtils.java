package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Utility functions related to Strings.
 */
public class StringUtils {

	private StringUtils() {
	}

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public static boolean isEmpty(String string) {
		return (string == null) || string.isEmpty();
	}

	/**
	 * Makes sure that the given string is not empty.
	 * 
	 * @param string
	 *            the string
	 * @return the string itself, or <code>null</code> if it is empty
	 */
	public static String getNotEmpty(String string) {
		return isEmpty(string) ? null : string;
	}

	/**
	 * Makes sure that the given string is not <code>null</code>.
	 * 
	 * @param string
	 *            the string
	 * @return the string itself, or an empty string if it is <code>null</code>
	 */
	public static String getOrEmpty(String string) {
		return (string == null) ? "" : string;
	}

	/**
	 * Converts the given {@link Object} to a String via its {@link #toString()} method.
	 * <p>
	 * If the object is <code>null</code>, or if its {@link #toString()} method returns <code>null</code>, this returns
	 * an empty String.
	 * 
	 * @param object
	 *            the object
	 * @return the String, not <code>null</code>
	 */
	public static String toStringOrEmpty(Object object) {
		if (object == null) return "";
		String string = object.toString();
		return (string != null) ? string : "";
	}

	/**
	 * Converts the given {@link Object} to a String via its {@link #toString()} method.
	 * <p>
	 * Returns <code>null</code> if the object is <code>null</code>, or if its {@link #toString()} method returns
	 * <code>null</code>.
	 * 
	 * @param object
	 *            the object
	 * @return the String, possibly <code>null</code>
	 */
	public static String toStringOrNull(Object object) {
		if (object == null) return null;
		return object.toString();
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace and converts all remaining whitespace and underscores to dashes
	 * ('{@code -}').
	 * <p>
	 * This returns {@link null} for a {@code null} input identifier.
	 * 
	 * @param identifier
	 *            the identifier
	 * @return the normalized identifier
	 */
	public static String normalizeKeepCase(String identifier) {
		if (identifier == null) return null;
		identifier = identifier.trim();
		identifier = identifier.replace('_', '-');
		identifier = replaceWhitespace(identifier, "-");
		return identifier;
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace and underscores to dashes
	 * ('{@code -}') and converts all characters to lower case.
	 * <p>
	 * This returns {@link null} for a {@code null} input identifier.
	 * 
	 * @param identifier
	 *            the identifier
	 * @return the normalized identifier
	 */
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
	 * Formats the given String in a way that is typical for enum names.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace and dashes to underscores
	 * ('{@code _}') and converts all characters to upper case.
	 * <p>
	 * This returns <code>null</code> if the input String is <code>null</code>.
	 * 
	 * @param enumName
	 *            the String to normalize
	 * @return the normalized enum name
	 */
	public static String normalizeEnumName(String enumName) {
		if (enumName == null) return null;
		enumName = enumName.trim();
		enumName = enumName.replace('-', '_');
		enumName = replaceWhitespace(enumName, "_");
		enumName = enumName.toUpperCase(Locale.ROOT);
		return enumName;
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

	public static String capitalizeAll(String source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		int sourceLength = source.length();
		StringBuilder builder = new StringBuilder(sourceLength);
		boolean capitalizeNext = true; // Capitalize first letter
		for (int i = 0; i < sourceLength; i++) {
			char currentChar = source.charAt(i);
			if (Character.isWhitespace(currentChar)) {
				capitalizeNext = true;
				builder.append(currentChar);
			} else if (capitalizeNext) {
				capitalizeNext = false;
				builder.append(Character.toTitleCase(currentChar));
			} else {
				builder.append(currentChar);
			}
		}
		return builder.toString();
	}

	// Matches any Unicode newlines (including the Windows newline sequence):
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
	// Additionally matches literal Unix newlines:
	private static final Pattern NEWLINE_OR_LITERAL_PATTERN = Pattern.compile("\\R|\\\\n");
	private static final Pattern ALL_TRAILING_NEWLINES_PATTERN = Pattern.compile("\\R+$");

	// Includes empty and trailing empty lines:
	public static String[] splitLines(String source) {
		return splitLines(source, false);
	}

	// Includes empty and trailing empty lines:
	public static String[] splitLines(String source, boolean splitLiteralNewlines) {
		if (splitLiteralNewlines) {
			return NEWLINE_OR_LITERAL_PATTERN.split(source, -1);
		} else {
			return NEWLINE_PATTERN.split(source, -1);
		}
	}

	public static String stripTrailingNewlines(String string) {
		Validate.notNull(string, "string is null");
		return ALL_TRAILING_NEWLINES_PATTERN.matcher(string).replaceFirst("");
	}

	public static boolean containsNewline(String string) {
		if (string == null) return false;
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
			case '\n': // Line feed (\\u000A)
			case '\r': // Carriage return (\\u000D)
			case '\f': // Form feed (\\u000C)
			case '\u000B': // Vertical tab
			case '\u0085': // Next line
			case '\u2028': // Unicode line separator
			case '\u2029': // Unicode paragraph separator
				return true;
			default:
				continue;
			}
		}
		return false;
	}

	public static String prefix(String prefix, String delimiter, String message) {
		if (StringUtils.isEmpty(prefix)) return message;
		return prefix + (delimiter == null ? "" : delimiter) + message;
	}

	// ARGUMENTS REPLACEMENT

	// Throws NPE if source, target, or replacement are null.
	// Returns the source String (same instance) if there is no match.
	// Performance is okay for replacing a single argument, but for multiple arguments prefer using replaceArguments
	// rather than invoking this multiple times.
	public static String replaceFirst(String source, String target, CharSequence replacement) {
		int index = source.indexOf(target);
		if (index == -1) return source; // No match

		int sourceLength = source.length();
		int targetLength = target.length();
		int increase = replacement.length() - targetLength;

		StringBuilder result = new StringBuilder(sourceLength + increase);
		result.append(source, 0, index); // Prefix
		result.append(replacement); // Replacement
		result.append(source, index + targetLength, sourceLength); // Suffix
		return result.toString();
	}

	private static final ArgumentsReplacer ARGUMENTS_REPLACER = new ArgumentsReplacer(); // Singleton for reuse
	private static final Map<String, Object> TEMP_ARGUMENTS_MAP = new HashMap<>();

	// Arguments format: [key1, value1, key2, value2, ...]
	// The keys are expected to be of type String.
	// The replaced keys use the format {key} (braces are not specified in the argument keys).
	public static <T> void addArgumentsToMap(Map<String, Object> argumentsMap, Object... argumentPairs) {
		Validate.notNull(argumentsMap, "argumentsMap is null");
		Validate.notNull(argumentPairs, "argumentPairs is null");
		Validate.isTrue(argumentPairs.length % 2 == 0, "argumentPairs.length is not a multiple of 2");
		int argumentsKeyLimit = argumentPairs.length - 1;
		for (int i = 0; i < argumentsKeyLimit; i += 2) {
			String key = (String) argumentPairs[i];
			Object value = argumentPairs[i + 1];
			argumentsMap.put(key, value);
		}
	}

	public static String replaceArguments(String source, Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(source, TEMP_ARGUMENTS_MAP);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // Reset
		}
	}

	// The replaced keys use the format {key} (braces are not specified in the argument keys).
	// Uses the String representation of the given arguments.
	// If an argument is a Supplier, it gets invoked to obtain the actual argument.
	public static String replaceArguments(String source, Map<String, Object> arguments) {
		return ARGUMENTS_REPLACER.replaceArguments(source, arguments); // Checks arguments
	}

	public static List<String> replaceArguments(Collection<String> messages, Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(messages, TEMP_ARGUMENTS_MAP);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // Reset
		}
	}

	// Creates and returns a new List:
	public static List<String> replaceArguments(Collection<String> sources, Map<String, Object> arguments) {
		Validate.notNull(sources, "sources is null!");
		List<String> replaced = new ArrayList<>(sources.size());
		for (String source : sources) {
			replaced.add(replaceArguments(source, arguments)); // Checks arguments
		}
		return replaced;
	}

	// Faster than regex/matcher and StringBuilder#replace in a loop.
	// Argument's map: Faster than iterating and comparing.
	// Arguments may occur more than once.
	// Arguments inside arguments are not replaced.
	// TODO Add support for argument options (eg.: formatting options, etc.).
	// TODO Allow escaping, eg. via "\{key\}"?
	// TODO Improve handling of inner braces: "{some{key}", "{some{inner}key}"
	public static class ArgumentsReplacer {

		// Default key format: {key}
		public static final char DEFAULT_KEY_PREFIX_CHAR = '{';
		public static final char DEFAULT_KEY_SUFFIX_CHAR = '}';

		private String source;
		private int sourceLength;
		private char keyPrefixChar;
		private char keySuffixChar;
		private Map<String, Object> arguments;

		// Current search state:
		private int searchPos = 0; // Index of where to start the search for the next key in the source String
		private int keyPrefixIndex = -1; // Index of last found key prefix char
		private int keySuffixIndex = -1; // Index of last found key suffix char
		private int keyStartIndex = -1; // Inclusive: points to first key char (after prefix char)
		private int keyEndIndex = -1; // Exclusive (= keySuffixIndex)
		protected String key = null; // The current key
		protected Object argument = null; // The current argument

		// Current result state:
		protected StringBuilder resultBuilder = null;
		// Start index of remaining source text that still needs to be included in the result:
		private int resultSourcePos = 0;
		private String result = null;

		public ArgumentsReplacer() {
		}

		public String replaceArguments(String source, Map<String, Object> arguments) {
			return this.replaceArguments(source, DEFAULT_KEY_PREFIX_CHAR, DEFAULT_KEY_SUFFIX_CHAR, arguments);
		}

		public String replaceArguments(String source, char keyPrefixChar, char keySuffixChar, Map<String, Object> arguments) {
			// Setup:
			this.setup(source, keyPrefixChar, keySuffixChar, arguments); // Validates input

			// Replace arguments:
			this.replaceArguments();

			// Capture result:
			String result = this.result;

			// Clean up (avoids accidental memory leaks in case this ArgumentReplacer is kept around for later reuse):
			this.cleanUp();

			// Return result:
			return result;
		}

		protected void setup(String source, char keyPrefixChar, char keySuffixChar, Map<String, Object> arguments) {
			Validate.notNull(source, "Source is null!");
			Validate.notNull(arguments, "Arguments is null!");

			this.source = source;
			sourceLength = source.length();
			this.keyPrefixChar = keyPrefixChar;
			this.keySuffixChar = keySuffixChar;
			this.arguments = arguments;

			// Current search state:
			searchPos = 0;
			keyPrefixIndex = -1;
			keySuffixIndex = -1;
			keyStartIndex = -1;
			keyEndIndex = -1;
			key = null;
			argument = null;

			// Current result state:
			result = null;
			if (resultBuilder != null) {
				resultBuilder.setLength(0); // We reuse the previous StringBuilder (note: keeps the current capacity)
			}
			resultSourcePos = 0; // Start index of remaining source text that still needs to be included in the result

			// Initial:
			if (sourceLength <= 2) {
				// Source is not large enough to contain any key: End the search right away.
				searchPos = sourceLength;
			}
		}

		// Clears any Object references that are no longer required after operation.
		protected void cleanUp() {
			source = null;
			arguments = null;
			key = null;
			argument = null;
			result = null;
			if (resultBuilder != null) {
				resultBuilder.setLength(0); // We reuse the previous StringBuilder (Note: Keeps the current capacity)
			}
		}

		private void replaceArguments() {
			assert result == null; // We don't have a result yet
			while (this.findNextKey()) {
				// Find replacement argument for the current key:
				argument = this.resolveArgument(key);

				// Append replacement:
				if (argument != null) {
					this.appendPrefix();
					this.appendArgument();
				} // Else: No argument found for the current key. -> Continue the search for the next key.
			}

			// Append remaining suffix (if any):
			if (resultSourcePos <= 0) {
				// There has been no argument replacement, otherwise we would have included a prefix to the result
				// already. -> The 'suffix' matches the complete source String.
				// We skip copying the suffix to the result builder and instead use the source directly as result.
				result = source;
				resultSourcePos = sourceLength; // Update resultSourcePos
				return;
			}

			if (resultSourcePos < sourceLength) {
				this.appendSuffix();
			} // Else: Remaining suffix is empty.

			// Prepare result:
			this.prepareResult();
		}

		// Returns true if a next key has been found.
		private boolean findNextKey() {
			if (searchPos >= sourceLength) return false; // We already searched through the whole source String

			// Search key prefix character:
			keyPrefixIndex = source.indexOf(keyPrefixChar, searchPos);
			if (keyPrefixIndex < 0) {
				return false; // No prefix char found. -> No more keys.
			}
			keyStartIndex = keyPrefixIndex + 1;

			// search key suffix character:
			keySuffixIndex = source.indexOf(keySuffixChar, keyStartIndex);
			if (keySuffixIndex < 0) {
				return false; // No suffix char found. -> No more keys.
			}
			keyEndIndex = keySuffixIndex;
			key = source.substring(keyStartIndex, keyEndIndex);

			// The search for the next key starts after the current key:
			searchPos = keySuffixIndex + 1;
			return true;
		}

		// The argument to replace the current key:
		protected Object resolveArgument(String key) {
			Object argument = arguments.get(key);
			if (argument instanceof Supplier) {
				return ((Supplier<?>) argument).get(); // Can be null
			} else {
				return argument; // Can be null
			}
		}

		protected void appendPrefix() {
			// Initialize result StringBuilder:
			if (resultBuilder == null) {
				// Heuristic: Expecting at most 25% increase in size.
				resultBuilder = new StringBuilder(sourceLength + sourceLength / 4);
			}

			// Append prefix (not yet included chars in front of key):
			resultBuilder.append(source, resultSourcePos, keyPrefixIndex);
			resultSourcePos = keySuffixIndex + 1; // Update resultSourcePos
		}

		protected void appendArgument() {
			assert resultBuilder != null; // We have already appended the prefix
			assert argument != null;
			// Append argument:
			String argumentString = argument.toString();
			resultBuilder.append(argumentString);
		}

		protected void appendSuffix() {
			assert resultSourcePos > 0 && resultSourcePos < sourceLength && resultBuilder != null;
			resultBuilder.append(source, resultSourcePos, sourceLength); // Append suffix
			resultSourcePos = sourceLength; // Update resultSourcePos
		}

		protected void prepareResult() {
			assert resultBuilder != null;
			result = resultBuilder.toString();
		}
	}
}
