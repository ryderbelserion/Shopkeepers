package com.nisovin.shopkeepers.config.lib.value;

import java.lang.reflect.Type;
import java.util.function.Function;

import com.nisovin.shopkeepers.util.Validate;

public class ValueTypeProviders {

	public static ValueTypeProvider forTypePattern(TypePattern typePattern, Function<Type, ValueType<?>> valueTypeProvider) {
		Validate.notNull(typePattern, "typePattern is null");
		Validate.notNull(valueTypeProvider, "valueTypeProvider is null");
		return new ValueTypeProvider() {
			@Override
			public ValueType<?> get(Type type) {
				if (typePattern.matches(type)) {
					return valueTypeProvider.apply(type);
				} else {
					return null;
				}
			}
		};
	}

	private ValueTypeProviders() {
	}
}
