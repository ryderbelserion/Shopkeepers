package com.nisovin.shopkeepers.util.data.serialization.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Number} values of various types.
 */
public final class NumberSerializers {

	private static abstract class NumberSerializer<N extends Number>
			implements DataSerializer<@NonNull N> {

		private final String numberTypeName;

		public NumberSerializer(Class<N> numberType) {
			Validate.notNull(numberType, "numberType is null");
			this.numberTypeName = numberType.getSimpleName();
		}

		@Override
		public @Nullable Object serialize(N value) {
			Validate.notNull(value, "value is null");
			return value;
		}

		@Override
		public N deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			@Nullable N value = this.deserializeNumber(data);
			if (value == null) {
				throw new InvalidDataException("Failed to parse " + numberTypeName
						+ " from '" + data + "'!");
			} else {
				return value;
			}
		}

		/**
		 * Converts the given data to a number of the target type.
		 * <p>
		 * This is invoked by {@link #deserialize(Object)}. Implementations may either return
		 * <code>null</code> to indicate a failure of reconstructing the number from the given data,
		 * which results in {@link #deserialize(Object)} to throw an {@link InvalidDataException}
		 * with a generic error message, or they may throw their own {@link InvalidDataException}.
		 * 
		 * @param data
		 *            the data, not <code>null</code>
		 * @return the number, or <code>null</code> if the number cannot be reconstructed (optional)
		 * @throws InvalidDataException
		 *             if the number cannot be reconstructed (optional)
		 */
		protected abstract @Nullable N deserializeNumber(Object data) throws InvalidDataException;
	}

	/**
	 * A {@link DataSerializer} for {@link Integer} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} tries to convert the given data to an {@link Integer}.
	 */
	public static final DataSerializer<Integer> INTEGER = new NumberSerializer<Integer>(
			Integer.class
	) {
		@Override
		public @Nullable Integer deserializeNumber(Object data) throws InvalidDataException {
			return ConversionUtils.toInteger(data);
		}
	};

	/**
	 * A {@link DataSerializer} for {@link Float} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} tries to convert the given data to a {@link Float}.
	 */
	public static final DataSerializer<Float> FLOAT = new NumberSerializer<Float>(Float.class) {
		@Override
		public @Nullable Float deserializeNumber(Object data) throws InvalidDataException {
			return ConversionUtils.toFloat(data);
		}
	};

	/**
	 * A {@link DataSerializer} for {@link Double} values.
	 * <p>
	 * During {@link DataSerializer#deserialize(Object) deserialization}, this
	 * {@link DataSerializer} tries to convert the given data to a {@link Double}.
	 */
	public static final DataSerializer<Double> DOUBLE = new NumberSerializer<Double>(Double.class) {
		@Override
		public @Nullable Double deserializeNumber(Object data) throws InvalidDataException {
			return ConversionUtils.toDouble(data);
		}
	};

	private NumberSerializers() {
	}
}
