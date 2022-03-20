package com.nisovin.shopkeepers.text;

import com.nisovin.shopkeepers.util.java.Validate;

public class ClickEventText extends TextBuilder {

	public enum Action {
		/**
		 * Prompts the player to open the specified url in his web browser.
		 */
		OPEN_URL,
		/**
		 * Prompts the player to open the file at the specified path.
		 */
		OPEN_FILE,
		/**
		 * Causes the player to enter and send the specified text in chat. This can be used to
		 * execute commands (starting with {@code /}). The player requires the corresponding command
		 * permission.
		 */
		RUN_COMMAND,
		/**
		 * Inserts the given string (not required to be a command) into the player's text box.
		 * <p>
		 * Does not work for signs or books.
		 * <p>
		 * Unlike insertion, this replaces the existing contents of the chat input.
		 */
		SUGGEST_COMMAND,
		/**
		 * Interprets the text as page number and switches to the corresponding book page.
		 * <p>
		 * Only works inside books.
		 */
		CHANGE_PAGE
	}

	private final Action action; // Not null
	private final String value; // Not null, can be empty

	ClickEventText(Action action, String value) {
		Validate.notNull(action, "action is null");
		Validate.notNull(value, "value is null");
		this.action = action;
		this.value = value;
	}

	// CLICK EVENT

	/**
	 * Gets the {@link ClickEventText.Action}.
	 * 
	 * @return the click event action, not <code>null</code>
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Gets the click event value.
	 * 
	 * @return the click event value, not <code>null</code>
	 */
	public String getValue() {
		return value;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		ClickEventText copy = new ClickEventText(action, value);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", action=");
		builder.append(this.getAction());
		builder.append(", value=");
		builder.append(this.getValue());
		super.appendToStringFeatures(builder);
	}
}
