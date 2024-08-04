package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

public class TextValue extends ValueType<Text> {

	public static final TextValue INSTANCE = new TextValue();

	public TextValue() {
	}

	@Override
	public @Nullable Text load(@Nullable Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		String stringValue = configValue.toString();
		return Text.parse(stringValue);
	}

	@Override
	public @Nullable Object save(@Nullable Text value) {
		if (value == null) return null;
		return value.toFormat();
	}

	@Override
	public String format(@Nullable Text value) {
		if (value == null) return "null";
		return value.toFormat();
	}

	@Override
	public Text parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		return Text.parse(input);
	}
}
