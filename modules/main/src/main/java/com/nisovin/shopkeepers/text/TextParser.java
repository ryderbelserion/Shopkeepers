package com.nisovin.shopkeepers.text;

import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Produces a {@link Text} from a plain String representation.
 * <p>
 * The primary goal of the mapping between the chosen String representation and its corresponding
 * parsed {@link Text} is to not only preserve the visual appearance, but also the structure, i.e.
 * the internal representation, of the given input text: Ideally, {@link Text#toPlainFormatText()}
 * of the parsed {@link Text} should be able to reproduce the original input.
 * <p>
 * The input String gets split at every color and formatting code, and at every placeholder, and
 * these segments are chained via {@link TextBuilder#next(Text)}.
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

			// Color codes:
			// TODO This does not account for hex colors. Bukkit's ChatColor is not able to
			// represent hex colors. We will need to switch to Spigot's BungeeCord ChatColor to
			// represent those.
			ChatColor color = null;
			if (i + 1 < length) {
				color = TextUtils.getChatColor(c, input.charAt(i + 1), true);
			}
			if (color != null) {
				// Append formatting:
				next(Text.formatting(color));

				i += 1; // Skip color character
				continue;
			} // Else: Continue and treat as regular character.

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

	private <T extends @NonNull TextBuilder> T next(T next) {
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
