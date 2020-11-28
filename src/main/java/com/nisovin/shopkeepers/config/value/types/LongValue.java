package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class LongValue extends ValueType<Long> {

	public static final LongValue INSTANCE = new LongValue();

	public LongValue() {
	}

	@Override
	public Long load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		Long value = ConversionUtils.toLong(configValue);
		if (value == null) {
			throw new SettingLoadException("Invalid long value: " + configValue);
		}
		return value;
	}

	@Override
	public Object save(Long value) {
		return value;
	}
}
