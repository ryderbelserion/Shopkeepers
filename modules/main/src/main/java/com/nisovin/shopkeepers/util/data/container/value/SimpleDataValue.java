package com.nisovin.shopkeepers.util.data.container.value;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A simple {@link DataValue} implementation that is not bound to any underlying data structure, but
 * stores the value itself.
 */
public class SimpleDataValue extends AbstractDataValue {

	private @Nullable Object value;

	/**
	 * Creates a new empty {@link SimpleDataValue}.
	 */
	public SimpleDataValue() {
		this(null);
	}

	/**
	 * Creates a new {@link SimpleDataValue}.
	 * 
	 * @param value
	 *            the initially stored value, or <code>null</code>
	 */
	public SimpleDataValue(@Nullable Object value) {
		this.value = value;
	}

	@Override
	public @Nullable Object getOrDefault(@Nullable Object defaultValue) {
		return (value != null) ? value : defaultValue;
	}

	@Override
	protected void internalSet(Object value) {
		this.value = value;
	}

	@Override
	public void clear() {
		this.value = null;
	}
}
