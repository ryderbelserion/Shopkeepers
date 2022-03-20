package com.nisovin.shopkeepers.util.data.container.value;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for {@link DataValue} implementations.
 */
public abstract class AbstractDataValue implements DataValue {

	private @Nullable DataValue view = null; // Lazily setup

	/**
	 * Creates a new {@link AbstractDataValue}.
	 */
	public AbstractDataValue() {
	}

	@Override
	public boolean isOfType(Class<?> type) {
		Validate.notNull(type, "type is null");
		Object value = this.get();
		return type.isInstance(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getOfTypeOrDefault(Class<T> type, @Nullable T defaultValue) {
		Validate.notNull(type, "type is null");
		Object value = this.get();
		return type.isInstance(value) ? (T) value : defaultValue;
	}

	@Override
	public @Nullable String getStringOrDefault(@Nullable String defaultValue) {
		String value = ConversionUtils.toString(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public int getIntOrDefault(int defaultValue) {
		Integer value = ConversionUtils.toInteger(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public long getLongOrDefault(long defaultValue) {
		Long value = ConversionUtils.toLong(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public float getFloatOrDefault(float defaultValue) {
		Float value = ConversionUtils.toFloat(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public double getDoubleOrDefault(double defaultValue) {
		Double value = ConversionUtils.toDouble(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public boolean getBooleanOrDefault(boolean defaultValue) {
		Boolean value = ConversionUtils.toBoolean(this.get());
		return (value != null) ? value : defaultValue;
	}

	@Override
	public void set(@Nullable Object value) {
		if (value == null) {
			this.clear();
		} else {
			// Storing a DataContainer or DataValue instead of its serialized form is a common
			// error:
			Validate.isTrue(!(value instanceof DataContainer), "Cannot insert DataContainer!");
			Validate.isTrue(!(value instanceof DataValue), "Cannot insert DataValue!");
			// Note: The value of this DataValue may be loaded from a storage format that supports
			// additional types of values. The validation of a value loaded from some storage is not
			// the responsibility of this DataValue, but of the clients that read the value from
			// this DataValue. We therefore do not validate or filter the inserted value here.
			this.internalSet(value);
		}
	}

	/**
	 * This method is invoked by {@link #set(Object)} after the value has been validated.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 */
	protected abstract void internalSet(Object value);

	@Override
	public DataValue asView() {
		if (view == null) {
			view = new UnmodifiableDataValue(this);
		}
		assert view != null;
		return view;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [value=");
		builder.append(this.get());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(this.get());
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DataValue)) return false;
		DataValue other = (DataValue) obj;
		return Objects.equals(this.get(), other.get());
	}
}
