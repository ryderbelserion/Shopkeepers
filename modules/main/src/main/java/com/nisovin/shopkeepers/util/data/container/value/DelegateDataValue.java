package com.nisovin.shopkeepers.util.data.container.value;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for {@link DataValue} implementations that delegate to another {@link DataValue}.
 */
public class DelegateDataValue extends AbstractDataValue {

	protected final DataValue dataValue;

	/**
	 * Creates a new {@link DelegateDataValue}.
	 * 
	 * @param dataValue
	 *            the underlying {@link DataValue}, not <code>null</code>
	 */
	public DelegateDataValue(DataValue dataValue) {
		Validate.notNull(dataValue, "dataValue is null");
		this.dataValue = dataValue;
	}

	@Override
	public @Nullable Object getOrDefault(@Nullable Object defaultValue) {
		return dataValue.getOrDefault(defaultValue);
	}

	@Override
	public void set(@Nullable Object value) {
		dataValue.set(value);
	}

	@Override
	protected void internalSet(Object value) {
		// Not expected to be called, because we override #set(String).
		throw new IllegalStateException("This method is not expected to be called!");
	}

	@Override
	public void clear() {
		dataValue.clear();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [dataValue=");
		builder.append(dataValue);
		builder.append("]");
		return builder.toString();
	}
}
