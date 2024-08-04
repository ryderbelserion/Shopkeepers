package com.nisovin.shopkeepers.config.lib.value.types;

public class StringListValue extends ListValue<String> {

	public static final StringListValue INSTANCE = new StringListValue();

	public StringListValue() {
		super(StringValue.INSTANCE);
	}
}
