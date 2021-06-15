package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ConversionUtils;

/**
 * A {@link Property} that stores a {@link String} value.
 */
public class StringProperty extends Property<String> {

	/**
	 * Creates a new {@link StringProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public StringProperty(AbstractShopkeeper shopkeeper, String key, String defaultValue) {
		super(shopkeeper, key, defaultValue);
	}

	@Override
	protected String loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(key);
		if (value == null) return null;
		String stringValue = ConversionUtils.toString(value);
		if (stringValue == null) {
			throw this.invalidValueError(value);
		} else {
			return stringValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, String value) {
		configSection.set(key, value);
	}
}
