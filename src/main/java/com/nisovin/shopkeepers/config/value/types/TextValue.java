package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.text.Text;

public class TextValue extends ValueType<Text> {

	public static final TextValue INSTANCE = new TextValue();

	private static final ColoredStringValue coloredStringValue = new ColoredStringValue();

	public TextValue() {
	}

	@Override
	public Text load(Object configValue) throws SettingLoadException {
		return Text.parse(coloredStringValue.load(configValue));
	}

	@Override
	public Object save(Text value) {
		if (value == null) return null;
		return coloredStringValue.save(value.toPlainFormatText());
	}
}
