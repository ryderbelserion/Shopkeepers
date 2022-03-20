package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StringListValue extends ListValue<@NonNull String> {

	public static final StringListValue INSTANCE = new StringListValue();

	public StringListValue() {
		super(StringValue.INSTANCE);
	}
}
