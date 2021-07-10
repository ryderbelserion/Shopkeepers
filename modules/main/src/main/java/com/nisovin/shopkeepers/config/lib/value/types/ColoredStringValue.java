package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ColoredStringValue extends StringValue {

	public static final ColoredStringValue INSTANCE = new ColoredStringValue();

	public ColoredStringValue() {
	}

	@Override
	public String load(Object configValue) throws ValueLoadException {
		return TextUtils.colorize(super.load(configValue));
	}

	@Override
	public Object save(String value) {
		return TextUtils.decolorize(value);
	}

	@Override
	public String format(String value) {
		if (value == null) return "null";
		return TextUtils.decolorize(value);
	}

	@Override
	public String parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		return TextUtils.colorize(input);
	}
}
