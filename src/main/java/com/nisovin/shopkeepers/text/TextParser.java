package com.nisovin.shopkeepers.text;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.util.TextUtils;

class TextParser {

	private static final TextParser INSTANCE = new TextParser();

	// Non-public, use Text#parse(String)
	// Note: Unlike Spigot, this does not take into account clickable URLs. URLs need to be made clickable manually
	// where required. TODO Include URL parsing?
	static Text parse(String input) {
		return INSTANCE._parse(input);
	}

	/////

	/*
	 * Primary goal: Persist not only the visual appearance but also the structure (internal representation) of the given input
	 * text. Text#toPlainText should produce the original input Text again if possible.
	 * 
	 * The input text gets split at every color and formatting code and every placeholder and chained via TextBuilder#next(Text).
	 */

	private TextBuilder root;
	private TextBuilder last;

	private final StringBuilder stringBuilder = new StringBuilder();

	private TextParser() {
	}

	private void reset() {
		root = null;
		last = null;
		stringBuilder.setLength(0);
	}

	private Text _parse(String input) {
		// Assert: Already reset.
		if (input == null) return null;
		if (input.isEmpty()) return Text.EMPTY;

		final int length = input.length();
		for (int i = 0; i < length; ++i) {
			char c = input.charAt(i);

			// Color codes:
			ChatColor color = null;
			if ((c == ChatColor.COLOR_CHAR || c == TextUtils.COLOR_CHAR_ALTERNATIVE) && i + 1 < length) {
				char colorChar = Character.toLowerCase(input.charAt(i + 1));
				color = ChatColor.getByChar(colorChar);
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

		assert root != null; // Expecting at least one Text since we checked for empty input
		Text result = root.build();
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
			assert last != null;
			last.next(next);
		}
		last = next;
		return next;
	}
}
