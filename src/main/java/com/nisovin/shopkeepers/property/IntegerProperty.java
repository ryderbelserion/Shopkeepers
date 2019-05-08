package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class IntegerProperty extends Property<Integer> {

	private final int minValue;
	private final int maxValue;

	public IntegerProperty(String key, Integer defaultValue) {
		this(key, Integer.MIN_VALUE, Integer.MAX_VALUE, defaultValue);
	}

	public IntegerProperty(String key, int minValue, int maxValue, Integer defaultValue) {
		super(key, defaultValue);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public final int getMinValue() {
		return minValue;
	}

	public final int getMaxValue() {
		return maxValue;
	}

	public final boolean isInBounds(int value) {
		return (value >= minValue && value <= maxValue);
	}

	@Override
	protected Integer loadValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(this.key);
		if (value == null) return null;
		Integer intValue = ConversionUtils.toInteger(value);
		if (intValue == null) {
			throw this.invalidValueError(shopkeeper, value);
		} else {
			// check bounds:
			if (!this.isInBounds(intValue)) {
				throw this.invalidValueError(shopkeeper, intValue);
			}
			return intValue;
		}
	}

	@Override
	protected void saveValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection, Integer value) {
		configSection.set(this.key, value);
	}
}
