package com.nisovin.shopkeepers.util.data.serialization.java;

import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link UUID} values.
 */
public final class UUIDSerializers {

	private static abstract class UUIDSerializer implements DataSerializer<UUID> {
		@Override
		public @Nullable Object serialize(UUID value) {
			Validate.notNull(value, "value is null");
			return value.toString();
		}
	}

	/**
	 * A {@link DataSerializer} for {@link UUID} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} accounts for different formatting alternatives when trying to parse
	 * the {@link UUID}.
	 */
	public static final DataSerializer<UUID> LENIENT = new UUIDSerializer() {
		@Override
		public UUID deserialize(Object data) throws InvalidDataException {
			String uuidString = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
			UUID value = ConversionUtils.parseUUID(uuidString);
			if (value == null) {
				throw new InvalidDataException("Failed to parse UUID from '" + uuidString + "'!");
			} else {
				return value;
			}
		}
	};

	private UUIDSerializers() {
	}
}
