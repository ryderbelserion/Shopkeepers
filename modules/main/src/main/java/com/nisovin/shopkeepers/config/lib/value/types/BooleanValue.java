package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.java.ConversionUtils;

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

	@Override
	public Boolean parse(String input) throws ValueParseException {
		Boolean value = ConversionUtils.parseBoolean(input);
		if (value == null) {
			throw new ValueParseException("Invalid boolean value: " + input);
		}
		return value;
	}
}
