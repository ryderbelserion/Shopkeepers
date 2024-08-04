package com.nisovin.shopkeepers.config.lib.value.types;

import java.lang.reflect.Type;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.TypePattern;
import com.nisovin.shopkeepers.config.lib.value.TypePatterns;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.config.lib.value.ValueTypeProvider;
import com.nisovin.shopkeepers.config.lib.value.ValueTypeProviders;

public class ColoredStringListValue extends ListValue<String> {

	public static final ColoredStringListValue INSTANCE = new ColoredStringListValue();
	public static final TypePattern TYPE_PATTERN = TypePatterns.parameterized(List.class, String.class);
	public static final ValueTypeProvider PROVIDER = ValueTypeProviders.forTypePattern(TYPE_PATTERN, type -> INSTANCE);

	public static final class Provider implements ValueTypeProvider {
		@Override
		public @Nullable ValueType<?> get(Type type) {
			return PROVIDER.get(type);
		}
	}

	public ColoredStringListValue() {
		super(ColoredStringValue.INSTANCE);
	}
}
