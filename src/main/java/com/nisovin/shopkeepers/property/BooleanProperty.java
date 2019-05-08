package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ConversionUtils;

public class BooleanProperty extends Property<Boolean> {

	public BooleanProperty(String key, Boolean defaultValue) {
		super(key, defaultValue);
	}

	@Override
	protected Boolean loadValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(this.key);
		if (value == null) return null;
		Boolean booleanValue = ConversionUtils.toBoolean(value);
		if (booleanValue == null) {
			throw this.invalidValueError(shopkeeper, value);
		} else {
			return booleanValue;
		}
	}

	@Override
	protected void saveValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection, Boolean value) {
		configSection.set(this.key, value);
	}
}
