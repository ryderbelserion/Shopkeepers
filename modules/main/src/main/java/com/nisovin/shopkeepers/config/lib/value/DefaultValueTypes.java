package com.nisovin.shopkeepers.config.lib.value;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.lib.value.types.BooleanValue;
import com.nisovin.shopkeepers.config.lib.value.types.DoubleValue;
import com.nisovin.shopkeepers.config.lib.value.types.EntityTypeValue;
import com.nisovin.shopkeepers.config.lib.value.types.EnumValue;
import com.nisovin.shopkeepers.config.lib.value.types.FloatValue;
import com.nisovin.shopkeepers.config.lib.value.types.IntegerValue;
import com.nisovin.shopkeepers.config.lib.value.types.ItemDataValue;
import com.nisovin.shopkeepers.config.lib.value.types.ListValue;
import com.nisovin.shopkeepers.config.lib.value.types.LongValue;
import com.nisovin.shopkeepers.config.lib.value.types.MaterialValue;
import com.nisovin.shopkeepers.config.lib.value.types.SoundEffectValue;
import com.nisovin.shopkeepers.config.lib.value.types.StringValue;
import com.nisovin.shopkeepers.config.lib.value.types.TextValue;
import com.nisovin.shopkeepers.config.lib.value.types.TrileanValue;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.java.Trilean;

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
		registry.register(Float.class, FloatValue.INSTANCE);
		registry.register(float.class, FloatValue.INSTANCE);
		registry.register(Long.class, LongValue.INSTANCE);
		registry.register(long.class, LongValue.INSTANCE);

		registry.register(Trilean.class, TrileanValue.INSTANCE);

		registry.register(Text.class, TextValue.INSTANCE);
		registry.register(Material.class, MaterialValue.INSTANCE);
		registry.register(ItemData.class, ItemDataValue.INSTANCE);
		registry.register(SoundEffect.class, SoundEffectValue.INSTANCE);
		registry.register(EntityType.class, EntityTypeValue.INSTANCE);

		// The following more general value type providers are only used for types which didn't
		// match any of the above:

		registry.register(ValueTypeProviders.forTypePattern(
				TypePatterns.forBaseType(Enum.class),
				new Function<Type, ValueType<?>>() {
					@Override
					public ValueType<?> apply(@Nullable Type type) {
						assert type instanceof Class<?> && Enum.class.isAssignableFrom((Class<?>) type);
						Class<? extends Enum<?>> enumClass = Unsafe.castNonNull(type);
						return this.newEnumValueType(enumClass);
					}

					private <E extends Enum<E>> EnumValue<E> newEnumValueType(
							Class<? extends Enum<?>> enumClass
					) {
						return new EnumValue<>(Unsafe.cast(enumClass));
					}
				}
		));
		registry.register(ValueTypeProviders.forTypePattern(
				TypePatterns.forClass(List.class),
				(type) -> {
					assert type instanceof ParameterizedType;
					Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
					ValueType<?> elementValueType = DefaultValueTypes.get(elementType);
					if (elementValueType == null) {
						throw new IllegalArgumentException("Unsupported element type: "
								+ elementType.getTypeName());
					}
					return new ListValue<>(elementValueType);
				}
		));
	}

	public static <T> @Nullable ValueType<T> get(Type type) {
		return registry.getValueType(type);
	}

	private DefaultValueTypes() {
	}
}
