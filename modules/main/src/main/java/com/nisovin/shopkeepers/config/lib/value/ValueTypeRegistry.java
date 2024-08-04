package com.nisovin.shopkeepers.config.lib.value;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Registry of value types for setting types.
 */
public class ValueTypeRegistry {

	private final Map<Type, ValueType<?>> byType = new HashMap<>();
	// Ordered: The first successful provider is used.
	private final List<ValueTypeProvider> providers = new ArrayList<>();

	public ValueTypeRegistry() {
	}

	// Replaces any previously registered ValueType.
	public <T> void register(Type type, ValueType<? extends T> valueType) {
		Validate.notNull(type, "type is null");
		Validate.notNull(valueType, "valueType is null");
		byType.put(type, valueType);
	}

	public boolean hasCachedValueType(Type type) {
		return byType.containsKey(type);
	}

	public void register(TypePattern typePattern, ValueType<?> valueType) {
		Validate.notNull(typePattern, "typePattern is null");
		Validate.notNull(valueType, "valueType is null");
		this.register(ValueTypeProviders.forTypePattern(typePattern, (type) -> valueType));
	}

	public void register(ValueTypeProvider valueTypeProvider) {
		Validate.notNull(valueTypeProvider, "valueTypeProvider is null");
		providers.add(valueTypeProvider);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable ValueType<T> getValueType(Type type) {
		ValueType<T> valueType = (ValueType<T>) byType.get(type);
		if (valueType == null) {
			// Check providers:
			for (ValueTypeProvider provider : providers) {
				valueType = (ValueType<T>) provider.get(type);
				if (valueType != null) {
					// Cache result:
					this.register(type, valueType);
					break;
				} // Else: Continue searching.
			}
		}
		return valueType;
	}
}
