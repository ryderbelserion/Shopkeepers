package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueLoadException;
import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class BooleanValue extends ValueType<Boolean> {

	public static final BooleanValue INSTANCE = new BooleanValue();

	public BooleanValue() {
	}

	@Override
	public Boolean load(Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		Boolean value = ConversionUtils.toBoolean(configValue);
		if (value == null) {
			throw new ValueLoadException("Invalid boolean value: " + configValue);
		}
		return value;
	}

	@Override
	public Object save(Boolean value) {
		return value;
	}
}
