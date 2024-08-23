package com.nisovin.shopkeepers.text;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class FormattingText extends TextBuilder {

	// The ChatColor char, or the hex color in format "#aabbcc":
	private final String formattingCode; // Not null

	FormattingText(String formattingCode) {
		Validate.notEmpty(formattingCode, "formatting code is null or empty");
		Validate.isTrue(formattingCode.length() == 1 || TextUtils.isHexCode(formattingCode),
				() -> "Invalid formatting code: " + formattingCode);
		this.formattingCode = formattingCode;
	}

	// FORMATTING

	/**
	 * Gets the formatting code.
	 * <p>
	 * This can be the code of a {@link ChatColor#isColor() color}, a {@link ChatColor#isFormat()
	 * format}, {@link ChatColor#RESET}, or a hex color in the format "#aabbcc".
	 * 
	 * @return the formatting code, not <code>null</code>
	 */
	public String getFormattingCode() {
		return formattingCode;
	}

	private boolean isHexColor() {
		return formattingCode.length() > 1;
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		if (formatText) {
			builder.append(TextUtils.COLOR_CHAR_ALTERNATIVE);
			builder.append(this.getFormattingCode());
		} else {
			builder.append(ChatColor.COLOR_CHAR);
			if (this.isHexColor()) {
				builder.append(TextUtils.toBukkitHexCode(this.getFormattingCode(), ChatColor.COLOR_CHAR));
			} else {
				builder.append(this.getFormattingCode());
			}
		}
		super.appendPlainText(builder, formatText);
	}

	@Override
	public boolean isPlainTextEmpty() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		FormattingText copy = new FormattingText(formattingCode);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", formattingCode=");
		builder.append(this.getFormattingCode());
		super.appendToStringFeatures(builder);
	}
}
