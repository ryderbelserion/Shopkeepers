package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.java.ConversionUtils;

public class LongValue extends ValueType<Long> {

	public static final LongValue INSTANCE = new LongValue();

	public LongValue() {
	}

	@Override
	public @Nullable Long load(@Nullable Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		Long value = ConversionUtils.toLong(configValue);
		if (value == null) {
			throw new ValueLoadException("Invalid long value: " + configValue);
		}
		return value;
	}

	@Override
	public @Nullable Object save(@Nullable Long value) {
		return value;
	}

	@Override
	public Long parse(String input) throws ValueParseException {
		Long value = ConversionUtils.parseLong(input);
		if (value == null) {
			throw new ValueParseException("Invalid long value: " + input);
		}
		return value;
	}
}
