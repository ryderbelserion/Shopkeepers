package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link Property} that stores an {@link Integer} value.
 */
public class IntegerProperty extends Property<Integer> {

	private final int minValue;
	private final int maxValue;

	/**
	 * Creates a new {@link IntegerProperty} with a minimum value of {@link Integer#MIN_VALUE} and maximum value of
	 * {@link Integer#MAX_VALUE}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public IntegerProperty(AbstractShopkeeper shopkeeper, String key, Integer defaultValue) {
		this(shopkeeper, key, Integer.MIN_VALUE, Integer.MAX_VALUE, defaultValue);
	}

	/**
	 * Creates a new {@link IntegerProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param minValue
	 *            the minimum value
	 * @param maxValue
	 *            the maximum value
	 * @param defaultValue
	 *            the default value
	 */
	public IntegerProperty(AbstractShopkeeper shopkeeper, String key, int minValue, int maxValue, Integer defaultValue) {
		super(shopkeeper, key, defaultValue);
		this.minValue = minValue;
		this.maxValue = maxValue;
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
	 * Gets the maximum value.
	 * 
	 * @return the maximum value
	 */
	public final int getMaxValue() {
		return maxValue;
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
	public void setValue(Integer value) {
		Validate.isTrue(value == null || this.isInBounds(value), "value is out of bounds");
		super.setValue(value);
	}

	@Override
	protected Integer loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(key);
		if (value == null) return null;
		Integer intValue = ConversionUtils.toInteger(value);
		if (intValue == null) {
			throw this.invalidValueError(value);
		} else {
			// Check bounds:
			if (!this.isInBounds(intValue)) {
				throw this.invalidValueError(intValue);
			}
			return intValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, Integer value) {
		configSection.set(key, value);
	}
}
