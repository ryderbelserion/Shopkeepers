package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class TextValue extends ValueType<Text> {

	public static final TextValue INSTANCE = new TextValue();

	private static final ColoredStringValue coloredStringValue = new ColoredStringValue();

	public TextValue() {
	}

	@Override
	public Text load(Object configValue) throws ValueLoadException {
		return Text.parse(coloredStringValue.load(configValue));
	}

	@Override
	public Object save(Text value) {
		if (value == null) return null;
		return coloredStringValue.save(value.toPlainFormatText());
	}

	@Override
	public String format(Text value) {
		if (value == null) return "null";
		return TextUtils.decolorize(value.toPlainFormatText());
	}

	@Override
	public Text parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		return Text.parse(input);
	}
}
