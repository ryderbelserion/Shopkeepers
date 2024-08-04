package com.nisovin.shopkeepers.text;

import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * A {@link Text} that uses a dynamically set {@link #getPlaceholderArgument() placeholder argument}
 * as its child.
 * <p>
 * If no placeholder argument is set this Text acts like a plain Text that consists of its
 * {@link #getFormattedPlaceholderKey() formatted placeholder key}.
 * <p>
 * The placeholder's {@link #getChild() child} cannot be set manually.
 */
public class PlaceholderText extends TextBuilder {

	public static final char PLACEHOLDER_PREFIX_CHAR = '{';
	public static final char PLACEHOLDER_SUFFIX_CHAR = '}';

	private final String placeholderKey; // Not null or empty
	private final String formattedPlaceholderKey; // Not null or empty
	private @Nullable Text placeholderArgument = null; // Can be null, not unbuilt

	// Not intended for public use, use Text.placeholder(String) instead.
	PlaceholderText(String placeholderKey) {
		Validate.notEmpty(placeholderKey, "placeholderKey is null or empty");
		this.placeholderKey = placeholderKey;
		this.formattedPlaceholderKey = (PLACEHOLDER_PREFIX_CHAR + placeholderKey + PLACEHOLDER_SUFFIX_CHAR);
	}

	// PLACEHOLDER

	private UnsupportedOperationException unsupportedPlaceholderOperation() {
		return new UnsupportedOperationException(
				"This operation is not supported for placeholder Texts!"
		);
	}

	/**
	 * Gets the placeholder key.
	 * 
	 * @return the placeholder key, not <code>null</code> or empty
	 */
	public String getPlaceholderKey() {
		return placeholderKey;
	}

	/**
	 * Gets the {@link #getPlaceholderKey() placeholder key} in the format <code>{key}</code>.
	 * 
	 * @return the formatted placeholder key, not <code>null</code> or empty
	 */
	public String getFormattedPlaceholderKey() {
		return formattedPlaceholderKey;
	}

	/**
	 * Gets the argument {@link Text} that is currently assigned to this {@link PlaceholderText}.
	 * <p>
	 * This Text adopts all properties of the argument Text. If no placeholder argument is set this
	 * Text acts like a plain Text that consists of its {@link #getFormattedPlaceholderKey()
	 * formatted placeholder key}.
	 * 
	 * @return the argument Text, or <code>null</code> if no argument is set
	 */
	public @Nullable Text getPlaceholderArgument() {
		return placeholderArgument;
	}

	/**
	 * Checks if this {@link PlaceholderText} has an argument assigned currently.
	 * 
	 * @return <code>true</code> if an argument is assigned
	 */
	public boolean hasPlaceholderArgument() {
		return (placeholderArgument != null);
	}

	/**
	 * Sets the argument {@link Text} that is currently assigned to this {@link PlaceholderText}.
	 * <p>
	 * Any non-{@link Text} argument is first converted to a corresponding {@link Text} by using its
	 * {@link Object#toString() String representation}. If the argument is a {@link Supplier}, it is
	 * invoked to obtain the actual argument.
	 * <p>
	 * If the argument is a {@link Text} that has not yet been {@link TextBuilder#isBuilt() built},
	 * this method may build it.
	 * 
	 * @param placeholderArgument
	 *            the argument Text, or <code>null</code> to unset the argument
	 * @see #getPlaceholderArgument()
	 */
	public void setPlaceholderArgument(@Nullable Object placeholderArgument) {
		Text placeholderArgumentText = null;
		if (placeholderArgument != null) {
			Validate.isTrue(placeholderArgument != this,
					"placeholderArgument cannot be this Text itself");
			placeholderArgumentText = Text.of(placeholderArgument);
			Validate.isTrue(placeholderArgumentText.getParent() == null,
					"placeholderArgument is a non-root Text");

			// Build unbuilt argument:
			buildIfRequired(placeholderArgumentText);

			// TODO Set parent?
			// Not setting the parent allows reusing the same placeholder argument Text instances
			// more easily. Setting the parent would currently block the Text from being used as
			// placeholder argument in more than one Text at once.
		}
		this.placeholderArgument = placeholderArgumentText; // Can be null
	}

	// PLACEHOLDER ARGUMENTS

	@Override
	public Text setPlaceholderArguments(MessageArguments arguments) {
		// Temporarily clear placeholder argument (if any) to not delegate to it via child
		// delegation:
		Text prevArgument = this.getPlaceholderArgument();
		this.setPlaceholderArgument(null);

		try {
			// Handles validation and delegation:
			super.setPlaceholderArguments(arguments);

			Object argument = arguments.get(placeholderKey); // Can be null
			if (argument != null) {
				this.setPlaceholderArgument(argument);
				prevArgument = null; // Don't restore previous argument
			} // Else retain currently assigned argument if any
		} finally {
			// Restore previous placeholder argument:
			if (prevArgument != null) {
				this.setPlaceholderArgument(prevArgument);
			}
		}
		return this;
	}

	@Override
	public Text clearPlaceholderArguments() {
		// Clear first to not delegate to it via child delegation:
		this.setPlaceholderArgument(null);

		// Handles delegation:
		super.clearPlaceholderArguments();
		return this;
	}

	// CHILD

	@Override
	public @Nullable Text getChild() {
		return placeholderArgument; // Can be null
	}

	@Override
	public <T extends Text> T child(T child) {
		throw unsupportedPlaceholderOperation();
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		if (formatText || !this.hasPlaceholderArgument()) {
			// Temporarily clear placeholder argument (if any) to not append it via child
			// delegation:
			Text prevArgument = this.getPlaceholderArgument();
			this.setPlaceholderArgument(null);

			// Append formatted placeholder key:
			builder.append(this.getFormattedPlaceholderKey());

			try {
				super.appendPlainText(builder, formatText);
			} finally {
				// Restore previous placeholder argument:
				if (prevArgument != null) {
					this.setPlaceholderArgument(prevArgument);
				}
			}
		} else {
			super.appendPlainText(builder, formatText);
		}
	}

	// COPY

	@Override
	public Text copy() {
		PlaceholderText copy = new PlaceholderText(placeholderKey);
		copy.copy(this, true);
		return copy.build();
	}

	/**
	 * This will not copy the placeholder key. Any currently set placeholder argument gets (deeply)
	 * copied.
	 */
	@Override
	public PlaceholderText copy(Text sourceText, boolean copyChilds) {
		super.copy(sourceText, copyChilds);
		this.copyPlaceholderArgument(sourceText);
		return this;
	}

	@Override
	protected void copyChild(Text sourceText) {
		return; // Not copying child
	}

	protected void copyPlaceholderArgument(Text sourceText) {
		if (sourceText instanceof PlaceholderText) {
			PlaceholderText placeholderSourceText = (PlaceholderText) sourceText;
			Text placeholderArgument = placeholderSourceText.getPlaceholderArgument();
			this.setPlaceholderArgument(
					placeholderArgument != null ? placeholderArgument.copy() : null
			);
		}
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", placeholderKey=");
		builder.append(placeholderKey);
		// Note: Placeholder argument gets already appended as 'child'.
		super.appendToStringFeatures(builder);
	}
}
