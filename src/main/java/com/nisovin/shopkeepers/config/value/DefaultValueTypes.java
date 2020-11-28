package com.nisovin.shopkeepers.config.value;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.value.types.BooleanValue;
import com.nisovin.shopkeepers.config.value.types.DoubleValue;
import com.nisovin.shopkeepers.config.value.types.IntegerValue;
import com.nisovin.shopkeepers.config.value.types.ItemDataValue;
import com.nisovin.shopkeepers.config.value.types.ListValue;
import com.nisovin.shopkeepers.config.value.types.LongValue;
import com.nisovin.shopkeepers.config.value.types.MaterialValue;
import com.nisovin.shopkeepers.config.value.types.StringValue;
import com.nisovin.shopkeepers.config.value.types.TextValue;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ItemData;

/**
 * Registry of default value types of settings.
 */
public class DefaultValueTypes {

	private static final ValueTypeRegistry registry = new ValueTypeRegistry();

	static {
		registry.register(String.class, StringValue.INSTANCE);
		registry.register(Boolean.class, BooleanValue.INSTANCE);
		registry.register(boolean.class, BooleanValue.INSTANCE);
		registry.register(Integer.class, IntegerValue.INSTANCE);
		registry.register(int.class, IntegerValue.INSTANCE);
		registry.register(Double.class, DoubleValue.INSTANCE);
		registry.register(double.class, DoubleValue.INSTANCE);
		registry.register(Long.class, LongValue.INSTANCE);
		registry.register(long.class, LongValue.INSTANCE);

		registry.register(Text.class, TextValue.INSTANCE);
		registry.register(Material.class, MaterialValue.INSTANCE);
		registry.register(ItemData.class, ItemDataValue.INSTANCE);

		registry.register(ValueTypeProviders.forTypePattern(TypePatterns.forClass(List.class), (type) -> {
			assert type instanceof ParameterizedType;
			Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
			ValueType<?> elementValueType = DefaultValueTypes.get(elementType);
			if (elementValueType == null) {
				throw new IllegalArgumentException("Unsupported element type: " + elementType.getTypeName());
			}
			return new ListValue<>(elementValueType);
		}));
	}

	public static <T> ValueType<T> get(Type type) {
		return registry.getValueType(type);
	}

	private DefaultValueTypes() {
	}
}
