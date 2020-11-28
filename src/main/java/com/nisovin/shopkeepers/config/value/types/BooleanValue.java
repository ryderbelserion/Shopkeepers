package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class BooleanValue extends ValueType<Boolean> {

	public static final BooleanValue INSTANCE = new BooleanValue();

	public BooleanValue() {
	}

	@Override
	public Boolean load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		Boolean value = ConversionUtils.toBoolean(configValue);
		if (value == null) {
			throw new SettingLoadException("Invalid boolean value: " + configValue);
		}
		return value;
	}

	@Override
	public Object save(Boolean value) {
		return value;
	}
}
