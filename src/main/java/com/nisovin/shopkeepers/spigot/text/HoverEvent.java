package com.nisovin.shopkeepers.spigot.text;

import com.nisovin.shopkeepers.util.Validate;

public class HoverEvent {

	public enum Action {
		/**
		 * The hover text can be multi-line by using the newline character {@code \n}.
		 */
		SHOW_TEXT,
		/**
		 * Requires the hover text to be the item's stringified NBT data.
		 */
		SHOW_ITEM,
		/**
		 * Requires the hover text to be the entity's stringified NBT data.
		 */
		SHOW_ENTITY,
		/**
		 * TODO
		 */
		SHOW_ACHIEVEMENT;
	}

	private final Action action; // not null
	private final String value; // not null, can be empty, can include color codes and newlines

	// shortcut for SHOW_TEXT
	public HoverEvent(String value) {
		this(Action.SHOW_TEXT, value);
	}

	public HoverEvent(Action action, String value) {
		Validate.notNull(action, "Action is null!");
		Validate.notNull(value, "Value is null!");
		this.action = action;
		this.value = value;
	}

	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * @return the value (hover text)
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HoverEvent [action=");
		builder.append(action);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
