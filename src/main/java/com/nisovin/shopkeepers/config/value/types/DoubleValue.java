package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class DoubleValue extends ValueType<Double> {

	public static final DoubleValue INSTANCE = new DoubleValue();

	public DoubleValue() {
	}

	@Override
	public Double load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		Double value = ConversionUtils.toDouble(configValue);
		if (value == null) {
			throw new SettingLoadException("Invalid double value: " + configValue);
		}
		return value;
	}

	@Override
	public Object save(Double value) {
		return value;
	}
}
