package com.nisovin.shopkeepers.property;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.ConversionUtils;

/**
 * A {@link Property} that stores an {@link UUID}.
 */
public class UUIDProperty extends Property<UUID> {

	/**
	 * Creates a new {@link UUIDProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public UUIDProperty(AbstractShopkeeper shopkeeper, String key, UUID defaultValue) {
		super(shopkeeper, key, defaultValue);
	}

	@Override
	protected UUID loadValue(ConfigurationSection configSection) throws InvalidValueException {
		String uuidString = configSection.getString(key);
		if (uuidString == null) return null;
		UUID uuid = ConversionUtils.parseUUID(uuidString);
		if (uuid == null) {
			throw this.invalidValueError(uuidString);
		} else {
			return uuid;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, UUID value) {
		configSection.set(key, (value == null) ? null : value.toString());
	}
}
