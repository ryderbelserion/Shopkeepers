package com.nisovin.shopkeepers.config.lib.setting;

import java.lang.reflect.Field;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.Validate;

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

		// Make the field accessible (it is inaccessible by default):
		// This is for example required to access private fields.
		field.setAccessible(true);
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
	public @Nullable T getValue() {
		try {
			// Note: The config instance is ignored if the field is static.
			return (@Nullable T) field.get(config);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Could not get the value from the setting's field!", e);
		}
	}

	@Override
	public void setValue(@Nullable T value) throws ValueLoadException {
		Class<?> fieldType = field.getType();
		if (!ClassUtils.isAssignableFrom(fieldType, value)) {
			throw new ValueLoadException("Value is of wrong type: Got "
					+ (value != null ? value.getClass().getName() : "null") + ", expected "
					+ fieldType.getName());
		}

		try {
			// Note: The config instance is ignored if the field is static.
			field.set(config, value);
		} catch (Exception e) {
			throw new ValueLoadException("Could not set the value of the setting's field!", e);
		}
	}
}
