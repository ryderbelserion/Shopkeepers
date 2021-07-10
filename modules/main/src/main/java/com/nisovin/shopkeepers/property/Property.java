package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Stores a value associated with a specific {@link Shopkeeper}.
 * <p>
 * Provides operations to get, set, serialize, and deserialize the value.
 * <p>
 * By default, the value and default value are not allowed to be <code>null</code>. Override {@link #isNullable()} if
 * the value of this property is allowed to be <code>null</code>.
 *
 * @param <T>
 *            the type of the stored value
 */
public abstract class Property<T> {

	private final AbstractShopkeeper shopkeeper;
	protected final String key;
	protected final T defaultValue;
	protected T value;

	/**
	 * Creates a new {@link Property}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty, has to be unique among all properties associated with
	 *            a specific shopkeeper
	 * @param defaultValue
	 *            the default value, can be <code>null</code> if this property is {@link #isNullable() nullable}
	 */
	public Property(AbstractShopkeeper shopkeeper, String key, T defaultValue) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notEmpty(key, "key is null or empty");
		// TODO Validate that the key is valid for use as config key?
		Validate.isTrue(defaultValue != null || this.isNullable(), "defaultValue is null for non-nullable property");
		this.shopkeeper = shopkeeper;
		this.key = key;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	/**
	 * Gets the {@link Shopkeeper} this property is associated with.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	protected final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Gets the storage key.
	 * 
	 * @return the storage key
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Gets the default value.
	 * 
	 * @return the default value
	 */
	public final T getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(T value) {
		Validate.isTrue(value != null || this.isNullable(), "value is null for non-nullable property");
		this.value = value;
	}

	/**
	 * Converts the given value into a String that can for example be used in exception messages, or to represent the
	 * value to a user.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @return the String representation, not <code>null</code>
	 */
	public String toString(T value) {
		return String.valueOf(value);
	}

	/**
	 * Whether the value of this property can be <code>null</code>.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if the value can be <code>null</code>
	 */
	public boolean isNullable() {
		return false;
	}

	/**
	 * Creates an {@link InvalidValueException} that is used when the value is missing when this property is loaded.
	 * 
	 * @return the invalid value exception
	 */
	protected InvalidValueException missingValueError() {
		return new InvalidValueException("Shopkeeper " + shopkeeper.getId() + ": Missing value for property '" + key + "'.");
	}

	/**
	 * Creates an {@link InvalidValueException} that is used when an existing value for this property could not be
	 * loaded, or is rejected for some reason.
	 * 
	 * @param invalidValue
	 *            the invalid value
	 * @return the invalid value exception
	 */
	protected InvalidValueException invalidValueError(Object invalidValue) {
		return new InvalidValueException("Shopkeeper " + shopkeeper.getId() + ": Invalid value '" + String.valueOf(invalidValue)
				+ "' for property '" + key + "'.");
	}

	/**
	 * This is invoked prior to loading the property's value and may prepare the given {@link ConfigurationSection}, for
	 * example to apply data migrations.
	 * 
	 * @param configSection
	 *            the configuration section
	 */
	protected void migrate(ConfigurationSection configSection) {
		// Nothing by default.
	}

	/**
	 * Loads the value of this property from the given {@link ConfigurationSection}.
	 * <p>
	 * This first performs any necessary {@link #migrate(ConfigurationSection) data migrations} and then delegates the
	 * actual loading to {@link #loadValue(ConfigurationSection)}.
	 * <p>
	 * If the value cannot be loaded for some reason, a warning is logged, the {@link #getDefaultValue() default value}
	 * is used, and the {@link Shopkeeper} is marked as {@link AbstractShopkeeper#markDirty() dirty}.
	 * 
	 * @param configSection
	 *            the configuration section
	 */
	public void load(ConfigurationSection configSection) {
		this.migrate(configSection);
		T value = null;
		try {
			value = this.loadValue(configSection);
			if (value == null && !this.isNullable()) {
				throw this.missingValueError();
			}
		} catch (InvalidValueException e) {
			Log.warning(e.getMessage() + " Using '" + this.toString(defaultValue) + "' now.");
			shopkeeper.markDirty();
			value = defaultValue;
		}
		this.setValue(value);
	}

	/**
	 * Loads the property's value from the given {@link ConfigurationSection}.
	 * 
	 * @param configSection
	 *            the configuration section
	 * @return the loaded value, or <code>null</code> if the value is not present
	 * @throws InvalidValueException
	 *             if the value can not be loaded
	 */
	protected abstract T loadValue(ConfigurationSection configSection) throws InvalidValueException;

	/**
	 * Saves the value of this property into the given {@link ConfigurationSection}.
	 * 
	 * @param configSection
	 *            the configuration section
	 */
	public void save(ConfigurationSection configSection) {
		this.saveValue(configSection, value);
	}

	/**
	 * Saves the given value for this property into the given {@link ConfigurationSection}.
	 * 
	 * @param configSection
	 *            the configuration section
	 * @param value
	 *            the value, can be <code>null</code>
	 */
	protected abstract void saveValue(ConfigurationSection configSection, T value);
}
