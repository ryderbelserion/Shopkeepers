package com.nisovin.shopkeepers.util.data.serialization.java;

import java.util.Collection;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link String} values.
 */
public final class StringSerializers {

	private static abstract class StringSerializer implements DataSerializer<String> {
		@Override
		public @Nullable Object serialize(String value) {
			Validate.notNull(value, "value is null");
			return value;
		}

		protected static InvalidDataException invalidDataTypeException(Object data) {
			return new InvalidDataException("Data is not of type String, but "
					+ data.getClass().getName() + "!");
		}

		protected static InvalidDataException emptyDataException() {
			return new InvalidDataException("String data is empty!");
		}
	}

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} considers any data that is not of type {@link String} to be invalid.
	 */
	public static final DataSerializer<String> STRICT = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			if (!(data instanceof String)) {
				throw invalidDataTypeException(data);
			}
			return (String) data;
		}
	};

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} behaves like {@link #STRICT}, but {@link String#isEmpty() empty}
	 * Strings are considered invalid.
	 */
	public static final DataSerializer<String> STRICT_NON_EMPTY = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			String value = STRICT.deserialize(data);
			if (value.isEmpty()) {
				throw emptyDataException();
			}
			return value;
		}
	};

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} uses {@link Object#toString()} to convert any given data to
	 * {@link String}, unless the given data is of some type of {@link Collection} or
	 * {@link DataContainer#isDataContainer(Object) DataContainer source}.
	 */
	public static final DataSerializer<String> SCALAR = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			if (data instanceof Collection || DataContainer.isDataContainer(data)) {
				throw invalidDataTypeException(data);
			}
			return LENIENT.deserialize(data);
		}
	};

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} behaves like {@link #SCALAR}, but {@link String#isEmpty() empty}
	 * Strings are considered invalid.
	 */
	public static final DataSerializer<String> SCALAR_NON_EMPTY = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			String value = SCALAR.deserialize(data);
			if (value.isEmpty()) {
				throw emptyDataException();
			}
			return value;
		}
	};

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} uses {@link Object#toString()} to convert any given data to
	 * {@link String}.
	 */
	public static final DataSerializer<String> LENIENT = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			String value = StringUtils.toStringOrNull(data);
			if (value == null) {
				// Unlikely. Only the case if the object's toString method returned null. Printing
				// the object as String in the error message won't be useful either then.
				throw new InvalidDataException("Failed to convert data of type "
						+ data.getClass().getName() + " to String!");
			} else {
				return value;
			}
		}
	};

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} behaves like {@link #LENIENT}, but {@link String#isEmpty() empty}
	 * Strings are considered invalid.
	 */
	public static final DataSerializer<String> LENIENT_NON_EMPTY = new StringSerializer() {
		@Override
		public String deserialize(Object data) throws InvalidDataException {
			String value = LENIENT.deserialize(data);
			if (value.isEmpty()) {
				throw emptyDataException();
			}
			return value;
		}
	};

	private StringSerializers() {
	}
}
