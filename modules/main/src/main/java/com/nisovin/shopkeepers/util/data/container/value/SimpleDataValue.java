package com.nisovin.shopkeepers.util.data.container.value;

/**
 * A simple {@link DataValue} implementation that is not bound to any underlying data structure, but stores the value
 * itself.
 */
public class SimpleDataValue extends AbstractDataValue {

	private Object value;

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
	public SimpleDataValue(Object value) {
		this.value = value;
	}

	@Override
	public Object getOrDefault(Object defaultValue) {
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
