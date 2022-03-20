package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ColoredStringValue extends StringValue {

	public static final ColoredStringValue INSTANCE = new ColoredStringValue();

	public ColoredStringValue() {
	}

	@Override
	public @Nullable String load(@Nullable Object configValue) throws ValueLoadException {
		String string = super.load(configValue);
		if (string == null) return null;
		return TextUtils.colorize(string);
	}

	@Override
	public @Nullable Object save(@Nullable String value) {
		if (value == null) return null;
		return TextUtils.decolorize(value);
	}

	@Override
	public String format(@Nullable String value) {
		if (value == null) return "null";
		return TextUtils.decolorize(value);
	}

	@Override
	public String parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		return TextUtils.colorize(input);
	}
}
