package com.nisovin.shopkeepers.property;

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
	protected Boolean deserializeValue(Object dataObject) throws InvalidValueException {
		assert dataObject != null;
		Boolean value = ConversionUtils.toBoolean(dataObject);
		if (value == null) {
			throw new InvalidValueException("Failed to parse Boolean from '" + dataObject + "'.");
		} else {
			return value;
		}
	}

	@Override
	protected Object serializeValue(Boolean value) {
		return value;
	}
}
