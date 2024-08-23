package com.nisovin.shopkeepers.text;

import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Produces a {@link Text} from a plain String representation.
 * <p>
 * One important goal of the mapping between the chosen String representation and the corresponding
 * parsed {@link Text}s is to not only preserve the visual appearance of the resulting texts, but to
 * also be able to {@link Text#toFormat() convert} the parsed {@link Text}s back to String
 * representations that match the original inputs as closely as possible. There are, however, a few
 * exceptions to this:
 * <ul>
 * <li>Color and formatting codes may get normalized to their lower-case variant.
 * <li>Hex colors may get converted from format "&x&a&a&b&b&c&c" to format "&#aabbcc".
 * <li>{@link Text#toFormat()} uses '{@literal &}' as formatting character, but parsing supports
 * both '{@literal &}' and '§'.
 * </ul>
 * <p>
 * The input String is split at every color and formatting code, and at every placeholder. The
 * resulting segments are chained together via {@link TextBuilder#next(Text)}.
 */
class TextParser {

	private static final TextParser INSTANCE = new TextParser();

	// Non-public, use Text#parse(String)
	// Note: Unlike Spigot, this does not take into account clickable URLs. URLs need to be made
	// clickable manually where required. TODO Include URL parsing?
	static Text parse(String input) {
		return INSTANCE._parse(input);
	}

	/////

	private @Nullable TextBuilder root;
	private @Nullable TextBuilder last;

	private final StringBuilder stringBuilder = new StringBuilder();

	private TextParser() {
	}

	private void reset() {
		root = null;
		last = null;
		stringBuilder.setLength(0);
	}

	private Text _parse(String input) {
		Validate.notNull(input, "input is null");
		// Assert: Already reset.
		if (input.isEmpty()) return Text.EMPTY;

		final int length = input.length();
		for (int i = 0; i < length; ++i) {
			char c = input.charAt(i);

			// Formatting codes:
			if (TextUtils.isAnyColorChar(c) && i + 1 < length) {
				char c2 = input.charAt(i + 1);
				char c2Lower = Character.toLowerCase(c2);

				String formattingCode = null;
				int skip = 0; // Number of subsequent formatting characters to skip
				if (c2Lower == 'x') { // Hex color in Bukkit format: "&x&a&a&b&b&c&c"
					if (i + 13 < length) {
						String hexString = input.substring(i, i + 14);
						if (TextUtils.isBukkitHexCode(hexString)) {
							formattingCode = TextUtils.fromBukkitHexCode(hexString);
							skip = 13;
						}
					}
				} else if (c2 == '#') { // Hex color in hex format: "&#aabbcc"
					if (i + 7 < length) {
						String hexString = input.substring(i + 1, i + 8);
						if (TextUtils.isHexCode(hexString)) {
							formattingCode = hexString;
							skip = 7;
						}
					}
				} else {
					ChatColor color = ChatColor.getByChar(c2Lower);
					if (color != null) {
						// Note: Preserves the case of the input character.
						formattingCode = String.valueOf(c2);
						skip = 1;
					}
				}

				if (formattingCode != null) {
					// Append formatting (preserve case of input character):
					next(Text.formatting(formattingCode));

					i += skip; // Skip formatting character(s)
					continue;
				} // Else: Continue and treat as regular character(s).
			}

			// Placeholder:
			if (c == PlaceholderText.PLACEHOLDER_PREFIX_CHAR) {
				int placeholderEnd = input.indexOf(PlaceholderText.PLACEHOLDER_SUFFIX_CHAR, i + 1);
				if (placeholderEnd != -1) {
					String placeholderKey = input.substring(i + 1, placeholderEnd);
					if (!placeholderKey.isEmpty()) {
						// Append placeholder:
						next(Text.placeholder(placeholderKey));

						i = placeholderEnd; // Skip the characters involved in the placeholder
						continue;
					}
				}
				// Else: Continue and treat as regular character.
			}

			// Regular text:
			stringBuilder.append(c);
		}

		// Append any remaining pending text:
		this.appendCurrentText();

		// We expect there to be at least one Text (root), because we checked for an empty input:
		Text result = Unsafe.assertNonNull(root).build();
		// Assert: All Texts in the chain are built.

		this.reset(); // Reset for later reuse
		return result;
	}

	private void appendCurrentText() {
		if (stringBuilder.length() > 0) {
			String text = stringBuilder.toString();
			stringBuilder.setLength(0); // Reset StringBuilder
			next(Text.text(text));
		}
	}

	private <T extends TextBuilder> T next(T next) {
		assert next != null;
		// Append any pending text:
		this.appendCurrentText();

		if (root == null) {
			// This is our first Text:
			root = next;
		} else {
			// Link to previous Text:
			Unsafe.assertNonNull(last).next(next);
		}
		last = next;
		return next;
	}
}
