package com.nisovin.shopkeepers.text;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A Text containing text.
 */
public class TextText extends TextBuilder {

	private final String text; // not null, can be empty

	TextText(String text) {
		Validate.notNull(text, "Text is null!");
		this.text = text;
	}

	// TEXT

	/**
	 * Gets the text.
	 * <p>
	 * Ideally this should not contain legacy color codes, because those can cause issues in certain edge cases (eg.
	 * with inheritance of text formatting and texts spanning multiple lines). But this may not be enforced currently.
	 * 
	 * @return the text, not <code>null</code>, can be empty
	 */
	public String getText() {
		return text;
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		builder.append(this.getText());
		super.appendPlainText(builder, formatText);
	}

	@Override
	public boolean isPlainTextEmpty() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		TextText copy = new TextText(text);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", text=");
		builder.append(this.getText());
		super.appendToStringFeatures(builder);
	}
}
