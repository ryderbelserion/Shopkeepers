package com.nisovin.shopkeepers.util.data;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataValue} that provides a view on the value of a {@link DataContainer} for a particular key.
 * <p>
 * The {@link DataValue} reads and writes through to the underlying entry of the data container.
 */
public class DataContainerValue extends AbstractDataValue {

	protected final DataContainer container;
	protected final String key;

	/**
	 * Creates a new {@link DataContainerValue}.
	 * 
	 * @param container
	 *            the underlying data container not <code>null</code>
	 * @param key
	 *            the data key, not <code>null</code> or empty
	 */
	public DataContainerValue(DataContainer container, String key) {
		Validate.notNull(container, "container is null");
		Validate.notEmpty(key, "key is empty");
		this.container = container;
		this.key = key;
	}

	@Override
	public Object getOrDefault(Object defaultValue) {
		return container.getOrDefault(key, defaultValue);
	}

	@Override
	protected void internalSet(Object value) {
		container.set(key, value);
	}

	@Override
	public void clear() {
		container.remove(key);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataContainerValue [container=");
		builder.append(container);
		builder.append(", key=");
		builder.append(key);
		builder.append(", value=");
		builder.append(this.get());
		builder.append("]");
		return builder.toString();
	}
}
