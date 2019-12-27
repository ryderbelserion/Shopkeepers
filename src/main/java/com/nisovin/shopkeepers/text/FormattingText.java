package com.nisovin.shopkeepers.text;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.util.Validate;

public class FormattingText extends TextBuilder {

	private final ChatColor formatting; // not null

	FormattingText(ChatColor formatting) {
		Validate.notNull(formatting, "Formatting is null!");
		this.formatting = formatting;
	}

	// FORMATTING

	/**
	 * Gets the formatting.
	 * <p>
	 * This can be a {@link ChatColor#isColor() color}, a {@link ChatColor#isFormat() format}, or
	 * {@link ChatColor#RESET}.
	 * 
	 * @return the formatting, not <code>null</code>
	 */
	public ChatColor getFormatting() {
		return formatting;
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		builder.append(this.getFormatting().toString());
		super.appendPlainText(builder, formatText);
	}

	@Override
	public boolean isPlainTextEmpty() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		FormattingText copy = new FormattingText(formatting);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", formatting=");
		builder.append(this.getFormatting());
		super.appendToStringFeatures(builder);
	}
}
