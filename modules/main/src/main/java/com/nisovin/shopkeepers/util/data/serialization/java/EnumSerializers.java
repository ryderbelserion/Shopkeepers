package com.nisovin.shopkeepers.util.data.serialization.java;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Enum} values.
 */
public final class EnumSerializers {

	public static abstract class EnumSerializer<E extends Enum<E>> implements DataSerializer<E> {

		protected final Class<E> enumType;

		/**
		 * Creates a new {@link EnumSerializers}.
		 * 
		 * @param enumType
		 *            the enum class, not <code>null</code>
		 */
		public EnumSerializer(Class<E> enumType) {
			Validate.notNull(enumType, "enumType is null");
			this.enumType = enumType;
		}

		protected final String deserializeEnumValueName(Object data) throws InvalidDataException {
			return StringSerializers.STRICT_NON_EMPTY.deserialize(data);
		}

		protected InvalidDataException unknownEnumValueError(String valueName) {
			assert valueName != null;
			return new InvalidDataException("Unknown " + enumType.getSimpleName() + ": " + valueName);
		}

		@Override
		public Object serialize(E value) {
			Validate.notNull(value, "value is null");
			return value.name();
		}
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified enum.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this {@link DataSerializer} uses
	 * {@link Enum#valueOf(Class, String)} to find an exact match for a given serialized enum value name.
	 * 
	 * @param <E>
	 *            the enum type
	 * @param enumType
	 *            the enum class, not <code>null</code>
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Enum<E>> DataSerializer<E> strict(Class<E> enumType) {
		return new EnumSerializer<E>(enumType) {
			@Override
			public E deserialize(Object data) throws InvalidDataException {
				String valueName = this.deserializeEnumValueName(data);
				E value = EnumUtils.valueOf(enumType, valueName);
				if (value == null) {
					throw this.unknownEnumValueError(valueName);
				} else {
					return value;
				}
			}
		};
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified enum.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this {@link DataSerializer} first tries to
	 * find an enum value whose name perfectly matches the given serialized enum value name, and otherwise tries to find
	 * a matching enum value by {@link EnumUtils#normalizeEnumName(String) formatting} the given serialized enum value
	 * name so that it matches the usual enum formatting.
	 * 
	 * @param <E>
	 *            the enum type
	 * @param enumType
	 *            the enum class, not <code>null</code>
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Enum<E>> DataSerializer<E> lenient(Class<E> enumType) {
		return new EnumSerializer<E>(enumType) {
			@Override
			public E deserialize(Object data) throws InvalidDataException {
				String valueName = this.deserializeEnumValueName(data);
				E value = ConversionUtils.parseEnum(enumType, valueName);
				if (value == null) {
					throw this.unknownEnumValueError(valueName);
				} else {
					return value;
				}
			}
		};
	}

	private EnumSerializers() {
	}
}
