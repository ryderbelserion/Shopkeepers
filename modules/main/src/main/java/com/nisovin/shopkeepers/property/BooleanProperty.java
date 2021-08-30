package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.java.ConversionUtils;

/**
 * A {@link Property} that stores a {@link Boolean} value.
 */
public class BooleanProperty extends Property<Boolean> {

	/**
	 * Creates a new {@link BooleanProperty}.
	 */
	public BooleanProperty() {
	}

	@Override
	protected Boolean loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(this.getKey());
		if (value == null) return null;
		Boolean booleanValue = ConversionUtils.toBoolean(value);
		if (booleanValue == null) {
			throw new InvalidValueException("Failed to parse Boolean: '" + value + "'.");
		} else {
			return booleanValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, Boolean value) {
		configSection.set(this.getKey(), value);
	}
}
