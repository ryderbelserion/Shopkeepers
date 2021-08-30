package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.java.ConversionUtils;

/**
 * A {@link Property} that stores a {@link String} value.
 */
public class StringProperty extends Property<String> {

	/**
	 * Creates a new {@link StringProperty}.
	 */
	public StringProperty() {
	}

	@Override
	protected String loadValue(ConfigurationSection configSection) throws InvalidValueException {
		Object value = configSection.get(this.getKey());
		if (value == null) return null;
		String stringValue = ConversionUtils.toString(value);
		if (stringValue == null) {
			// Unlikely. Only the case if the object's toString method returned null. Printing the object as String in
			// the error message won't be useful then either.
			throw new InvalidValueException("Failed to load String from object of type " + value.getClass().getName() + ".");
		} else {
			return stringValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, String value) {
		configSection.set(this.getKey(), value);
	}
}
