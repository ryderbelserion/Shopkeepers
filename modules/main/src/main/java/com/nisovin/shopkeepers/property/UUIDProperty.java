package com.nisovin.shopkeepers.property;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

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
	protected UUID loadValue(ConfigurationSection configSection) throws InvalidValueException {
		String uuidString = configSection.getString(this.getKey());
		if (uuidString == null) return null;
		UUID uuid = ConversionUtils.parseUUID(uuidString);
		if (uuid == null) {
			throw new InvalidValueException("Failed to parse UUID: '" + uuidString + "'.");
		} else {
			return uuid;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, UUID value) {
		configSection.set(this.getKey(), (value == null) ? null : value.toString());
	}
}
