package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link Property} that stores an {@link Integer} value.
 * <p>
 * It is possible to limit the property's value into specific {@link #getMinValue() lower} and {@link #getMaxValue()
 * upper} bounds. If this property is {@link #isNullable() nullable} these bounds are only checked for
 * non-<code>null</code> values.
 */
public class IntegerProperty extends Property<Integer> {

	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;

	/**
	 * Creates a new {@link IntegerProperty}.
	 */
	public IntegerProperty() {
	}

	@Override
	protected void postConstruct() {
		super.postConstruct();
		Validate.State.isTrue(minValue <= maxValue, () -> "Minimum value is greater than maximum value (min: "
				+ minValue + ", max: " + maxValue + ")!");
	}

	/**
	 * Gets the minimum value.
	 * 
	 * @return the minimum value
	 */
	public final int getMinValue() {
		return minValue;
	}

	/**
	 * Sets the minimum value of this property.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build(PropertyContainer) built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param minValue
	 *            the minimum value
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends IntegerProperty> P minValue(int minValue) {
		this.validateNotBuilt();
		this.minValue = minValue;
		return (P) this;
	}

	/**
	 * Gets the maximum value.
	 * 
	 * @return the maximum value
	 */
	public final int getMaxValue() {
		return maxValue;
	}

	/**
	 * Sets the maximum value of this property.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build(PropertyContainer) built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param maxValue
	 *            the maximum value
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends IntegerProperty> P maxValue(int maxValue) {
		this.validateNotBuilt();
		this.maxValue = maxValue;
		return (P) this;
	}

	/**
	 * Checks if the given value is inside the bounds of this {@link IntegerProperty}.
	 * 
	 * @param value
	 *            the value
	 * @return <code>true</code> if the value is inside the bounds
	 */
	public final boolean isInBounds(int value) {
		return value >= minValue && value <= maxValue;
	}

	@Override
	protected void internalValidateValue(Integer value) {
		super.internalValidateValue(value);
		Validate.isTrue(value == null || this.isInBounds(value), () -> "Value is out of bounds: " + value);
	}

	@Override
	protected Integer loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(this.getKey());
		if (value == null) return null;
		Integer intValue = ConversionUtils.toInteger(value);
		if (intValue == null) {
			throw new InvalidValueException("Failed to parse Integer: '" + value + "'.");
		} else {
			return intValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, Integer value) {
		configSection.set(this.getKey(), value);
	}
}
