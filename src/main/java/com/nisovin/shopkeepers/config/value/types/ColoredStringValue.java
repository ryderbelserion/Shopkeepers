package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.util.TextUtils;

public class ColoredStringValue extends StringValue {

	public static final ColoredStringValue INSTANCE = new ColoredStringValue();

	public ColoredStringValue() {
	}

	@Override
	public String load(Object configValue) throws SettingLoadException {
		return TextUtils.colorize(super.load(configValue));
	}

	@Override
	public Object save(String value) {
		return TextUtils.decolorize(value);
	}
}
