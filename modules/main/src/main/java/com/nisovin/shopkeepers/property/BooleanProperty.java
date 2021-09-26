package com.nisovin.shopkeepers.property;

import com.nisovin.shopkeepers.util.data.InvalidDataException;
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
	protected Boolean deserializeValue(Object dataObject) throws InvalidDataException {
		assert dataObject != null;
		Boolean value = ConversionUtils.toBoolean(dataObject);
		if (value == null) {
			throw new InvalidDataException("Failed to parse Boolean from '" + dataObject + "'.");
		} else {
			return value;
		}
	}

	@Override
	protected Object serializeValue(Boolean value) {
		return value;
	}
}
