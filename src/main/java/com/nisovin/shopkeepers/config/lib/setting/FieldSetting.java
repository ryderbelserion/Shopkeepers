package com.nisovin.shopkeepers.config.lib.setting;

import java.lang.reflect.Field;

import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Setting} that wraps a {@link Field} of a config class.
 * <p>
 * Getting and setting the value of this Setting accesses the value of the wrapped field.
 *
 * @param <T>
 *            the value type
 */
public class FieldSetting<T> implements Setting<T> {

	private final Config config;
	private final Field field;
	private final String configKey;
	private final ValueType<T> valueType;

	public FieldSetting(Config config, Field field, String configKey, ValueType<T> valueType) {
		Validate.notNull(config, "config is null");
		Validate.notNull(field, "field is null");
		Validate.notEmpty(configKey, "configKey is null or empty");
		Validate.notNull(valueType, "valueType is null");
		this.config = config;
		this.field = field;
		this.configKey = configKey;
		this.valueType = valueType;
	}

	@Override
	public Config getConfig() {
		return config;
	}

	/**
	 * Gets the wrapped {@link Field}
	 * 
	 * @return the field
	 */
	public Field getField() {
		return field;
	}

	@Override
	public String getConfigKey() {
		return configKey;
	}

	@Override
	public ValueType<T> getValueType() {
		return valueType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getValue() {
		try {
			// Note: The config instance is ignored if the field is static.
			return (T) field.get(config);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void setValue(T value) throws ValueLoadException {
		if (value != null) {
			Class<?> fieldType = field.getType();
			if (!Utils.isAssignableFrom(fieldType, value.getClass())) {
				throw new ValueLoadException("Value is of wrong type: Got " + value.getClass().getName()
						+ ", expected " + fieldType.getName());
			}
		}

		boolean accessible = field.isAccessible();
		try {
			if (!accessible) {
				// Temporarily set the field accessible, for example for private fields:
				field.setAccessible(true);
			}
			// Note: The config instance is ignored if the field is static.
			field.set(config, value);
		} catch (Exception e) {
			throw new ValueLoadException(e.getMessage(), e);
		} finally {
			// Restore previous accessible state:
			try {
				field.setAccessible(accessible);
			} catch (SecurityException e) {
			}
		}
	}
}
