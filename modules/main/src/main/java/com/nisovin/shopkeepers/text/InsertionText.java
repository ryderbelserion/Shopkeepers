package com.nisovin.shopkeepers.text;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * When shift-clicked by the player, the insertion text gets inserted into his chat input.
 * <p>
 * Unlike {@link net.md_5.bungee.api.chat.ClickEvent.Action#SUGGEST_COMMAND} this does not replace
 * the already existing chat input.
 */
public class InsertionText extends TextBuilder {

	private final String insertion; // Not null or empty

	InsertionText(String insertion) {
		Validate.notEmpty(insertion, "insertion is null or empty");
		this.insertion = insertion;
	}

	// INSERTION

	/**
	 * Gets the insertion text.
	 * 
	 * @return the insertion text, not <code>null</code> or empty
	 */
	public String getInsertion() {
		return insertion;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		InsertionText copy = new InsertionText(insertion);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", insertion=");
		builder.append(this.getInsertion());
		super.appendToStringFeatures(builder);
	}
}
