package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.MinecraftEnumUtils;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers.EnumSerializer;

/**
 * Default {@link DataSerializer}s for Bukkit {@link Enum} values that are based on Minecaft
 * namespaced ids, such as {@link Material}.
 * <p>
 * These serializers behaves similar to the normal {@link EnumSerializers}, but may take into
 * account any formatting differences of these Bukkit enums.
 */
public final class MinecraftEnumSerializers {

	private static class LenientMinecraftEnumSerializer<E extends Enum<E>>
			extends EnumSerializer<@NonNull E> {

		/**
		 * Creates a new {@link LenientMinecraftEnumSerializer}.
		 * 
		 * @param enumType
		 *            the enum class, not <code>null</code>
		 */
		public LenientMinecraftEnumSerializer(Class<@NonNull E> enumType) {
			super(enumType);
		}

		@Override
		public @NonNull E deserialize(Object data) throws InvalidDataException {
			String valueName = this.deserializeEnumValueName(data);
			@Nullable E value = MinecraftEnumUtils.parseEnum(enumType, valueName);
			if (value == null) {
				throw this.unknownEnumValueError(valueName);
			} else {
				return value;
			}
		}
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified enum.
	 * <p>
	 * The returned serializer behaves like {@link EnumSerializers#strict(Class)}.
	 * 
	 * @param <E>
	 *            the enum type
	 * @param enumType
	 *            the enum class, not <code>null</code>
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Enum<E>> DataSerializer<@NonNull E> strict(
			Class<@NonNull E> enumType
	) {
		return EnumSerializers.strict(enumType);
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified enum.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} first tries to find an enum value whose name perfectly matches the
	 * given serialized enum value name, and otherwise tries to find a matching enum value by
	 * {@link MinecraftEnumUtils#normalizeEnumName(String) formatting} the given serialized enum
	 * value name so that it matches the usual formatting of Bukkit enums.
	 * 
	 * @param <E>
	 *            the enum type
	 * @param enumType
	 *            the enum class, not <code>null</code>
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Enum<E>> DataSerializer<@NonNull E> lenient(
			Class<@NonNull E> enumType
	) {
		return new LenientMinecraftEnumSerializer<@NonNull E>(enumType);
	}

	/**
	 * Default {@link DataSerializer}s for {@link Material} {@link Enum} values.
	 */
	public static final class Materials {

		/**
		 * A {@link DataSerializer} for {@link Material} values.
		 * <p>
		 * This {@link DataSerializer} behaves similar to the lenient data serializers returned by
		 * {@link MinecraftEnumSerializers#lenient(Class)}. Besides the common enum name formatting,
		 * this data serializer performs no conversions of the material name during deserialization,
		 * but assumes an up-to-date material name.
		 * <p>
		 * If no matching {@link Material} is found during deserialization, this data serializer
		 * throws an {@link UnknownMaterialException} instead of a normal
		 * {@link InvalidDataException}.
		 */
		public static final DataSerializer<Material> LENIENT = new LenientMinecraftEnumSerializer<Material>(
				Material.class
		) {
			@Override
			protected InvalidDataException unknownEnumValueError(String valueName) {
				assert valueName != null;
				return new UnknownMaterialException("Unknown material: " + valueName);
			}
		};

		private Materials() {
		}
	}

	/**
	 * Default {@link DataSerializer}s for {@link EquipmentSlot} {@link Enum} values.
	 */
	public static final class EquipmentSlots {
		/**
		 * A {@link MinecraftEnumSerializers#lenient(Class) lenient} {@link DataSerializer} for
		 * {@link EquipmentSlot} values.
		 */
		public static final DataSerializer<EquipmentSlot> LENIENT = lenient(EquipmentSlot.class);
	}

	private MinecraftEnumSerializers() {
	}
}
