package com.nisovin.shopkeepers.util.data.serialization.java;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link DataContainer} values.
 */
public final class DataContainerSerializers {

	private static abstract class DataContainerSerializer implements DataSerializer<DataContainer> {
		@Override
		public @Nullable Object serialize(DataContainer value) {
			Validate.notNull(value, "value is null");
			return value.serialize();
		}
	}

	/**
	 * A {@link DataSerializer} for {@link DataContainer} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} uses {@link DataContainer#of(Object)} to reconstruct the data
	 * container.
	 */
	public static final DataSerializer<DataContainer> DEFAULT = new DataContainerSerializer() {
		@Override
		public DataContainer deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			DataContainer value = DataContainer.of(data);
			if (value == null) {
				throw new InvalidDataException("Data is not a DataContainer, but of type "
						+ data.getClass().getName() + "!");
			} else {
				return value;
			}
		}
	};

	/**
	 * A {@link DataSerializer} for {@link DataContainer} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} behaves like {@link #DEFAULT}, but {@link DataContainer#isEmpty()
	 * empty} data containers are considered invalid.
	 */
	public static final DataSerializer<DataContainer> DEFAULT_NON_EMPTY = new DataContainerSerializer() {
		@Override
		public DataContainer deserialize(Object data) throws InvalidDataException {
			DataContainer value = DEFAULT.deserialize(data);
			if (value.isEmpty()) {
				throw new InvalidDataException("Data is empty!");
			} else {
				return value;
			}
		}
	};

	private DataContainerSerializers() {
	}
}
