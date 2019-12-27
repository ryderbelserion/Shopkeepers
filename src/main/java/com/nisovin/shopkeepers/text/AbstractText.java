package com.nisovin.shopkeepers.text;

/**
 * Base class for all {@link Text} implementations.
 */
public abstract class AbstractText implements Text {

	// TODO remove parent reference?
	// would allow less mutable state, which simplifies reuse of Text instances

	private Text parent = null;

	// TODO cache plain text? requires childs to inform parents on changes to their translation or placeholder arguments
	// -> might not even be worth it in the presence of dynamic arguments

	protected AbstractText() {
	}

	// PARENT

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Text> T getParent() {
		// note: allows the caller to conveniently cast the result to the expected Text type (eg. to TextBuilder in a
		// fluently built Text)
		return (T) parent;
	}

	/**
	 * Sets the parent.
	 * <p>
	 * Internal method that is meant to only be used by Text implementations!
	 * 
	 * @param parent
	 *            the parent, can be <code>null</code>
	 */
	public void setParent(Text parent) {
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Text> T getRoot() {
		Text text = this;
		while (text.getParent() != null) {
			text = text.getParent();
		}
		// note: allows the caller to conveniently cast the result to the expected Text type (eg. to TextBuilder in a
		// fluently built Text)
		return (T) text;
	}

	// PLAIN TEXT

	@Override
	public String toPlainText() {
		StringBuilder builder = new StringBuilder();
		this.appendPlainText(builder, false);
		return builder.toString();
	}

	@Override
	public String toPlainFormatText() {
		StringBuilder builder = new StringBuilder();
		this.appendPlainText(builder, true);
		return builder.toString();
	}

	protected abstract void appendPlainText(StringBuilder builder, boolean formatText);
}
