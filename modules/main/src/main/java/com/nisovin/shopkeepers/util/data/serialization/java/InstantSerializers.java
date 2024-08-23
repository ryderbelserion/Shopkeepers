package com.nisovin.shopkeepers.util.data.serialization.java;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Instant} values.
 */
public final class InstantSerializers {

	/**
	 * A {@link DataSerializer} for {@link Instant} values.
	 * <p>
	 * This uses {@link DateTimeFormatter#ISO_INSTANT} to represent and reconstruct the instant.
	 */
	public static final DataSerializer<Instant> ISO = new DataSerializer<Instant>() {
		@Override
		public @Nullable Object serialize(Instant value) {
			Validate.notNull(value, "value is null");
			return value.toString();
		}

		@Override
		public Instant deserialize(Object data) throws InvalidDataException {
			String instantString = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
			try {
				return Instant.parse(instantString);
			} catch (DateTimeParseException e) {
				throw new InvalidDataException("Failed to parse timestamp from '"
						+ instantString + "'!", e);
			}
		}
	};

	private InstantSerializers() {
	}
}
