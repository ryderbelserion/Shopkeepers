package com.nisovin.shopkeepers.property;

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
	protected String deserializeValue(Object dataObject) throws InvalidValueException {
		assert dataObject != null;
		String value = ConversionUtils.toString(dataObject);
		if (value == null) {
			// Unlikely. Only the case if the object's toString method returned null. Printing the object as String in
			// the error message won't be useful then either.
			throw new InvalidValueException("Failed to convert data of type "
					+ dataObject.getClass().getName() + " to String.");
		} else {
			return value;
		}
	}

	@Override
	protected Object serializeValue(String value) {
		return value;
	}
}
