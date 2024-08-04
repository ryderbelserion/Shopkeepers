package com.nisovin.shopkeepers.util.data.property.validation.java;

import com.nisovin.shopkeepers.util.data.property.validation.PropertyValidator;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link PropertyValidator}s for {@link Integer} values.
 */
public final class IntegerValidators {

	/**
	 * A {@link PropertyValidator} that ensures that the validated integer is within specific
	 * bounds.
	 */
	private static class BoundedIntegerValidator implements PropertyValidator<Integer> {

		private final int minValue;
		private final int maxValue;

		/**
		 * Creates a new {@link BoundedIntegerValidator}.
		 * 
		 * @param minValue
		 *            the minimum value
		 * @param maxValue
		 *            the maximum value
		 */
		public BoundedIntegerValidator(int minValue, int maxValue) {
			Validate.isTrue(minValue <= maxValue, () -> "minValue (" + minValue
					+ ") is greater than maxValue (" + maxValue + ")");
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		private boolean isInBounds(int value) {
			return value >= minValue && value <= maxValue;
		}

		@Override
		public void validate(Integer value) {
			Validate.notNull(value, "value is null");
			Validate.isTrue(this.isInBounds(value), () -> this.getOutOfBoundsMessage(value));
		}

		/**
		 * Gets the error message that is used if the validated value is out of bounds.
		 * 
		 * @param value
		 *            the value
		 * @return the error message, not <code>null</code> or empty
		 */
		protected String getOutOfBoundsMessage(int value) {
			return "Value is out of bounds: min=" + minValue + ", max=" + maxValue
					+ ", value" + value + ".";
		}
	}

	/**
	 * A {@link PropertyValidator} that ensures that the validated integer is positive.
	 */
	public static final PropertyValidator<Integer> POSITIVE = new BoundedIntegerValidator(
			1,
			Integer.MAX_VALUE
	) {
		@Override
		protected String getOutOfBoundsMessage(int value) {
			return "Value has to be positive, but is " + value + ".";
		}
	};

	/**
	 * A {@link PropertyValidator} that ensures that the validated integer is not negative.
	 */
	public static final PropertyValidator<Integer> NON_NEGATIVE = new BoundedIntegerValidator(
			0,
			Integer.MAX_VALUE
	) {
		@Override
		protected String getOutOfBoundsMessage(int value) {
			return "Value cannot be negative, but is " + value + ".";
		}
	};

	/**
	 * Get a {@link PropertyValidator} that ensures that the validated integer is within the
	 * specified bounds.
	 * 
	 * @param minValue
	 *            the minimum value
	 * @param maxValue
	 *            the maximum value
	 * @return the property validator
	 */
	public static PropertyValidator<Integer> bounded(int minValue, int maxValue) {
		return new BoundedIntegerValidator(minValue, maxValue);
	}

	private IntegerValidators() {
	}
}
