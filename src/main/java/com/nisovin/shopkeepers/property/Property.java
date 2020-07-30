package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public abstract class Property<T> {

	protected final String key;
	protected final T defaultValue;

	public Property(String key, T defaultValue) {
		Validate.notEmpty(key, "Empty key!");
		Validate.isTrue(defaultValue != null || this.isNullable(), "Default value is null for property '" + key + "'!");
		this.key = key;
		this.defaultValue = defaultValue;
	}

	public final String getKey() {
		return key;
	}

	public final T getDefaultValue() {
		return defaultValue;
	}

	public String toString(T value) {
		return String.valueOf(value);
	}

	public boolean isNullable() {
		return false;
	}

	protected InvalidValueException missingValueError(AbstractShopkeeper shopkeeper) {
		return new InvalidValueException("Shopkeeper " + shopkeeper.getId() + ": Missing value for property '" + key + "'.");
	}

	protected InvalidValueException invalidValueError(AbstractShopkeeper shopkeeper, Object invalidValue) {
		return new InvalidValueException("Shopkeeper " + shopkeeper.getId() + ": Invalid value '" + String.valueOf(invalidValue)
				+ "' for property '" + key + "'.");
	}

	protected void migrate(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) {
		// Nothing by default.
	}

	public T load(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) {
		this.migrate(shopkeeper, configSection);
		try {
			T value = this.loadValue(shopkeeper, configSection);
			if (value == null && !this.isNullable()) {
				throw this.missingValueError(shopkeeper);
			}
			return value;
		} catch (InvalidValueException e) {
			Log.warning(e.getMessage() + " Using '" + this.toString(defaultValue) + "' now.");
			shopkeeper.markDirty();
			return defaultValue;
		}
	}

	// Null is considered a valid value by this method.
	protected abstract T loadValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) throws InvalidValueException;

	public void save(AbstractShopkeeper shopkeeper, ConfigurationSection configSection, T value) {
		this.saveValue(shopkeeper, configSection, value);
	}

	// Value can be null.
	protected abstract void saveValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection, T value);
}
