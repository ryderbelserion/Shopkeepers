package com.nisovin.shopkeepers.util.data.serialization.java;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Boolean} values.
 */
public final class BooleanSerializers {

	private static abstract class BooleanSerializer implements DataSerializer<Boolean> {
		@Override
		public @Nullable Object serialize(Boolean value) {
			Validate.notNull(value, "value is null");
			return value;
		}
	}

	/**
	 * A {@link DataSerializer} for {@link Boolean} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} considers any data that is not of type {@link Boolean} to be invalid.
	 */
	public static final DataSerializer<Boolean> STRICT = new BooleanSerializer() {
		@Override
		public Boolean deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			if (!(data instanceof Boolean)) {
				throw new InvalidDataException("Data is not of type Boolean, but "
						+ data.getClass().getName() + "!");
			}
			return (Boolean) data;
		}
	};

	/**
	 * A {@link DataSerializer} for {@link Boolean} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} accounts for various alternative representations when trying to
	 * convert the given data to a {@link Boolean} value.
	 */
	public static final DataSerializer<Boolean> LENIENT = new BooleanSerializer() {
		@Override
		public Boolean deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			Boolean value = ConversionUtils.toBoolean(data);
			if (value == null) {
				throw new InvalidDataException("Failed to parse Boolean from '" + data + "'!");
			} else {
				return value;
			}
		}
	};

	private BooleanSerializers() {
	}
}
