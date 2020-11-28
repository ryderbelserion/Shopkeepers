package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.config.value.ValueType;

public class StringValue extends ValueType<String> {

	public static final StringValue INSTANCE = new StringValue();

	public StringValue() {
	}

	@Override
	public String load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		return configValue.toString();
	}

	@Override
	public Object save(String value) {
		return value;
	}
}
