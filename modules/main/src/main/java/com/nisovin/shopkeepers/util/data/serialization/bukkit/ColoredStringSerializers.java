package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for colored {@link String} values that automatically convert
 * between Minecraft's color codes and the {@link TextUtils#COLOR_CHAR_ALTERNATIVE alternative color
 * codes}.
 */
public final class ColoredStringSerializers {

	private static final class ColoredStringSerializer implements DataSerializer<String> {

		private final DataSerializer<String> stringSerializer;

		private ColoredStringSerializer(DataSerializer<String> stringSerializer) {
			Validate.notNull(stringSerializer, "stringSerializer is null");
			this.stringSerializer = stringSerializer;
		}

		@Override
		public @Nullable Object serialize(String value) {
			Validate.notNull(value, "value is null");
			String decolored = TextUtils.decolorize(value);
			return stringSerializer.serialize(decolored);
		}

		@Override
		public String deserialize(Object data) throws InvalidDataException {
			String value = stringSerializer.deserialize(data);
			return TextUtils.colorize(value);
		}
	}

	/**
	 * A {@link DataSerializer} for colored {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#STRICT}, but converts color
	 * codes.
	 */
	public static final DataSerializer<String> STRICT = new ColoredStringSerializer(
			StringSerializers.STRICT
	);

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#STRICT_NON_EMPTY}, but
	 * converts color codes.
	 */
	public static final DataSerializer<String> STRICT_NON_EMPTY = new ColoredStringSerializer(
			StringSerializers.STRICT_NON_EMPTY
	);

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#SCALAR}, but converts color
	 * codes.
	 */
	public static final DataSerializer<String> SCALAR = new ColoredStringSerializer(
			StringSerializers.SCALAR
	);

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#SCALAR_NON_EMPTY}, but
	 * converts color codes.
	 */
	public static final DataSerializer<String> SCALAR_NON_EMPTY = new ColoredStringSerializer(
			StringSerializers.SCALAR_NON_EMPTY
	);

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#LENIENT}, but converts
	 * color codes.
	 */
	public static final DataSerializer<String> LENIENT = new ColoredStringSerializer(
			StringSerializers.LENIENT
	);

	/**
	 * A {@link DataSerializer} for {@link String} values.
	 * <p>
	 * This {@link DataSerializer} behaves like {@link StringSerializers#LENIENT_NON_EMPTY}, but
	 * converts color codes.
	 */
	public static final DataSerializer<String> LENIENT_NON_EMPTY = new ColoredStringSerializer(
			StringSerializers.LENIENT_NON_EMPTY
	);

	private ColoredStringSerializers() {
	}
}
