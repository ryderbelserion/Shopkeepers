package com.nisovin.shopkeepers.util.data;

import java.util.Map;

import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for {@link DataContainer} implementations.
 */
public abstract class AbstractDataContainer implements DataContainer {

	private DataContainer view = null; // Lazily setup

	/**
	 * Creates a new {@link AbstractDataContainer}.
	 */
	public AbstractDataContainer() {
	}

	@Override
	public DataValue getDataValue(String key) {
		return new DataContainerValue(this, key);
	}

	@Override
	public boolean isOfType(String key, Class<?> type) {
		Validate.notNull(type, "type is null");
		Object value = this.get(key);
		return type.isInstance(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getOfTypeOrDefault(String key, Class<T> type, T defaultValue) {
		Validate.notNull(type, "type is null");
		Object value = this.get(key);
		return type.isInstance(value) ? (T) value : defaultValue;
	}

	@Override
	public String getStringOrDefault(String key, String defaultValue) {
		String value = ConversionUtils.toString(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public int getIntOrDefault(String key, int defaultValue) {
		Integer value = ConversionUtils.toInteger(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public long getLongOrDefault(String key, long defaultValue) {
		Long value = ConversionUtils.toLong(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public float getFloatOrDefault(String key, float defaultValue) {
		Float value = ConversionUtils.toFloat(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public double getDoubleOrDefault(String key, double defaultValue) {
		Double value = ConversionUtils.toDouble(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public boolean getBooleanOrDefault(String key, boolean defaultValue) {
		Boolean value = ConversionUtils.toBoolean(this.get(key));
		return (value != null) ? value : defaultValue;
	}

	@Override
	public void set(String key, Object value) {
		Validate.notEmpty(key, "key is empty");
		if (value == null) {
			this.remove(key);
		} else {
			// Storing a DataContainer or DataValue instead of its serialized form is a common error:
			Validate.isTrue(!(value instanceof DataContainer), "Cannot insert DataContainer!");
			Validate.isTrue(!(value instanceof DataValue), "Cannot insert DataValue!");
			// Note: The contents of this DataContainer may be loaded from a storage format that supports additional
			// types of values. The validation of values loaded from some storage is not the responsibility of this
			// DataContainer, but of the clients that read values from this DataContainer. We therefore do not validate
			// or filter the inserted values here.
			this.internalSet(key, value);
		}
	}

	/**
	 * This method is invoked by {@link #set(String, Object)} after the key and value have been validated.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param value
	 *            the value, not <code>null</code>
	 */
	protected abstract void internalSet(String key, Object value);

	@Override
	public void setAll(Map<?, ?> values) {
		Validate.notNull(values, "values is null");
		values.entrySet().forEach(entry -> {
			String key = ConversionUtils.toString(entry.getKey());
			Object value = entry.getValue();
			this.set(key, value);
		});
	}

	@Override
	public <T> T get(DataAccessor<T> accessor) throws InvalidDataException {
		Validate.notNull(accessor, "accessor is null");
		return accessor.load(this);
	}

	@Override
	public <T> T getOrNullIfMissing(DataAccessor<T> accessor) throws InvalidDataException {
		Validate.notNull(accessor, "accessor is null");
		try {
			return accessor.load(this);
		} catch (MissingDataException e) {
			return null;
		}
	}

	@Override
	public <T> T getOrNull(DataAccessor<T> accessor) {
		Validate.notNull(accessor, "accessor is null");
		try {
			return accessor.load(this);
		} catch (InvalidDataException e) {
			return null;
		}
	}

	@Override
	public <T> void set(DataAccessor<T> accessor, T value) {
		Validate.notNull(accessor, "accessor is null");
		accessor.save(this, value);
	}

	@Override
	public DataContainer asView() {
		if (view == null) {
			view = new UnmodifiableDataContainer(this);
		}
		return view;
	}
}
