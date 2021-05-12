package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ConversionUtils;

/**
 * A {@link Property} that stores a {@link Boolean} value.
 */
public class BooleanProperty extends Property<Boolean> {

	/**
	 * Creates a new {@link BooleanProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public BooleanProperty(AbstractShopkeeper shopkeeper, String key, Boolean defaultValue) {
		super(shopkeeper, key, defaultValue);
	}

	@Override
	protected Boolean loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(key);
		if (value == null) return null;
		Boolean booleanValue = ConversionUtils.toBoolean(value);
		if (booleanValue == null) {
			throw this.invalidValueError(value);
		} else {
			return booleanValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, Boolean value) {
		configSection.set(key, value);
	}
}
