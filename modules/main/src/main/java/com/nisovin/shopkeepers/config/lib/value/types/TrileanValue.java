package com.nisovin.shopkeepers.config.lib.value.types;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Trilean;

// We don't use a nullable Boolean here, because we use use null as an indicator for missing values
// or failed conversions in a few places.
// We don't use Optional, because Optional becomes verbose to work with.
public class TrileanValue extends ValueType<Trilean> {

	public static final TrileanValue INSTANCE = new TrileanValue();

	public TrileanValue() {
	}

	@Override
	public @Nullable Trilean load(@Nullable Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		@Nullable Trilean value = ConversionUtils.toTrilean(configValue);
		if (value == null) {
			throw new ValueLoadException("Invalid trilean value: " + configValue);
		}
		return value;
	}

	@Override
	public @Nullable Object save(@Nullable Trilean value) {
		if (value == null) return null;
		if (value == Trilean.UNDEFINED) return value.name().toLowerCase(Locale.ROOT);
		return value.toBoolean();
	}

	@Override
	public Trilean parse(String input) throws ValueParseException {
		@Nullable Trilean value = ConversionUtils.parseTrilean(input);
		if (value == null) {
			throw new ValueParseException("Invalid trilean value: " + input);
		}
		return value;
	}
}
