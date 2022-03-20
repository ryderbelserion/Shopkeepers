package com.nisovin.shopkeepers.util.data.container.value;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataValue} that provides a view on the value of a {@link DataContainer} for a particular
 * key.
 * <p>
 * The {@link DataValue} reads and writes through to the underlying entry of the data container.
 */
public class DataContainerValue extends AbstractDataValue {

	protected final DataContainer dataContainer;
	protected final String dataKey;

	/**
	 * Creates a new {@link DataContainerValue}.
	 * 
	 * @param dataContainer
	 *            the underlying data container not <code>null</code>
	 * @param dataKey
	 *            the data key, not <code>null</code> or empty
	 */
	public DataContainerValue(DataContainer dataContainer, String dataKey) {
		Validate.notNull(dataContainer, "dataContainer is null");
		Validate.notEmpty(dataKey, "dataKey is empty");
		this.dataContainer = dataContainer;
		this.dataKey = dataKey;
	}

	@Override
	public @Nullable Object getOrDefault(@Nullable Object defaultValue) {
		return dataContainer.getOrDefault(dataKey, defaultValue);
	}

	@Override
	protected void internalSet(Object value) {
		dataContainer.set(dataKey, value);
	}

	@Override
	public void clear() {
		dataContainer.remove(dataKey);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataContainerValue [dataContainer=");
		builder.append(dataContainer);
		builder.append(", dataKey=");
		builder.append(dataKey);
		builder.append(", value=");
		builder.append(this.get());
		builder.append("]");
		return builder.toString();
	}
}
