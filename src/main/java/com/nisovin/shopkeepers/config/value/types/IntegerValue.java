package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class IntegerValue extends ValueType<Integer> {

	public static final IntegerValue INSTANCE = new IntegerValue();

	public IntegerValue() {
	}

	@Override
	public Integer load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		Integer value = ConversionUtils.toInteger(configValue);
		if (value == null) {
			throw new SettingLoadException("Invalid integer value: " + configValue);
		}
		return value;
	}

	@Override
	public Object save(Integer value) {
		return value;
	}
}
