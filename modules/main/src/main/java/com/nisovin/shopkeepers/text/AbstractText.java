package com.nisovin.shopkeepers.text;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * Base class for all {@link Text} implementations.
 */
public abstract class AbstractText implements Text {

	// Reused among all Text instances:
	private static final Map<String, Object> TEMP_ARGUMENTS_MAP = new HashMap<>();
	private static final MessageArguments TEMP_ARGUMENTS = MessageArguments.ofMap(TEMP_ARGUMENTS_MAP);

	// TODO Remove parent reference?
	// Would allow less mutable state, which simplifies reuse of Text instances.
	private @Nullable Text parent = null;

	private @Nullable Text child = null;
	private @Nullable Text next = null;

	// TODO Cache plain text? Requires childs to inform parents on changes to their translation or
	// placeholder arguments. -> Might not even be worth it in the presence of dynamic arguments.

	protected AbstractText() {
	}

	// PARENT

	@Override
	public <T extends Text> @Nullable T getParent() {
		// Note: Allows the caller to conveniently cast the result to the expected Text type (e.g.
		// to TextBuilder in a fluently built Text).
		return Unsafe.cast(parent);
	}

	/**
	 * Sets the parent Text.
	 * <p>
	 * Internal method that is meant to only be used by Text implementations!
	 * 
	 * @param parent
	 *            the parent Text, can be <code>null</code>
	 */
	private void setParent(@Nullable Text parent) {
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Text> @NonNull T getRoot() {
		Text text = this;
		Text parent;
		while ((parent = text.getParent()) != null) {
			text = parent;
		}
		// Note: Allows the caller to conveniently cast the result to the expected Text type (e.g.
		// to TextBuilder in a fluently built Text).
		return (T) text;
	}

	// CHILD

	@Override
	public @Nullable Text getChild() {
		return child;
	}

	/**
	 * Sets the child Text.
	 * <p>
	 * Internal method that is meant to only be used by Text implementations!
	 * 
	 * @param child
	 *            the child Text, or <code>null</code> to unset it
	 */
	protected void setChild(@Nullable Text child) {
		if (child != null) {
			Validate.isTrue(child != this, "child cannot be this Text itself");
			Validate.isTrue(child.getParent() == null, "child already has a parent");
			((AbstractText) child).setParent(this);
		}
		if (this.child != null) {
			((AbstractText) this.child).setParent(null);
		}
		this.child = child; // Can be null
	}

	// NEXT

	@Override
	public @Nullable Text getNext() {
		return next;
	}

	/**
	 * Sets the next Text.
	 * <p>
	 * Internal method that is meant to only be used by Text implementations!
	 * 
	 * @param next
	 *            the next Text, or <code>null</code> to unset it
	 */
	protected void setNext(@Nullable Text next) {
		if (next != null) {
			Validate.isTrue(next != this, "next cannot be this Text itself");
			Validate.isTrue(next.getParent() == null, "next already has a parent");
			((AbstractText) next).setParent(this);
		}
		if (this.next != null) {
			((AbstractText) this.next).setParent(null);
		}
		this.next = next; // Can be null
	}

	// PLACEHOLDER ARGUMENTS

	@Override
	public Text setPlaceholderArguments(MessageArguments arguments) {
		Validate.notNull(arguments, "arguments is null");

		// Delegate to childs:
		Text child = this.getChild();
		if (child != null) {
			child.setPlaceholderArguments(arguments);
		}

		// Delegate to next:
		Text next = this.getNext();
		if (next != null) {
			next.setPlaceholderArguments(arguments);
		}
		return this;
	}

	@Override
	public final Text setPlaceholderArguments(Map<? extends String, @NonNull ?> arguments) {
		return this.setPlaceholderArguments(MessageArguments.ofMap(arguments));
	}

	@Override
	public final Text setPlaceholderArguments(@NonNull Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			StringUtils.addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return this.setPlaceholderArguments(TEMP_ARGUMENTS);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // Reset
		}
	}

	@Override
	public Text clearPlaceholderArguments() {
		// Delegate to childs:
		Text child = this.getChild();
		if (child != null) {
			child.clearPlaceholderArguments();
		}

		// Delegate to next:
		Text next = this.getNext();
		if (next != null) {
			next.clearPlaceholderArguments();
		}
		return this;
	}

	// PLAIN TEXT

	@Override
	public String toPlainText() {
		StringBuilder builder = new StringBuilder();
		this.appendPlainText(builder, false);
		return builder.toString();
	}

	@Override
	public String toFormat() {
		StringBuilder builder = new StringBuilder();
		this.appendPlainText(builder, true);
		return builder.toString();
	}

	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		// Child:
		Text child = this.getChild();
		if (child != null) {
			((AbstractText) child).appendPlainText(builder, formatText);
		}

		// Next:
		Text next = this.getNext();
		if (next != null) {
			((AbstractText) next).appendPlainText(builder, formatText);
		}
	}

	@Override
	public boolean isPlainText() {
		// Child:
		Text child = this.getChild();
		if (child != null && !child.isPlainText()) {
			return false;
		}

		// Next:
		Text next = this.getNext();
		if (next != null && !next.isPlainText()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isPlainTextEmpty() {
		// Child:
		Text child = this.getChild();
		if (child != null && !child.isPlainTextEmpty()) {
			return false;
		}

		// Next:
		Text next = this.getNext();
		if (next != null && !next.isPlainTextEmpty()) {
			return false;
		}
		return true;
	}

	// UNFORMATTED TEXT

	@Override
	public String toUnformattedText() {
		String unformatted = ChatColor.stripColor(this.toPlainText());
		return Unsafe.assertNonNull(unformatted);
	}
}
