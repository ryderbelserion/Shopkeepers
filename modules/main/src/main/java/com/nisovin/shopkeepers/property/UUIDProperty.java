package com.nisovin.shopkeepers.property;

import java.util.UUID;

import com.nisovin.shopkeepers.util.java.ConversionUtils;

/**
 * A {@link Property} that stores an {@link UUID}.
 */
public class UUIDProperty extends Property<UUID> {

	/**
	 * Creates a new {@link UUIDProperty}.
	 */
	public UUIDProperty() {
	}

	@Override
	protected UUID deserializeValue(Object dataObject) throws InvalidValueException {
		assert dataObject != null;
		if (!(dataObject instanceof String)) {
			throw new InvalidValueException("UUID data is not of type String, but "
					+ dataObject.getClass().getName() + ".");
		}

		String uuidString = (String) dataObject;
		UUID uuid = ConversionUtils.parseUUID(uuidString);
		if (uuid == null) {
			throw new InvalidValueException("Failed to parse UUID from '" + uuidString + "'.");
		} else {
			return uuid;
		}
	}

	@Override
	protected Object serializeValue(UUID value) {
		return value.toString();
	}
}
