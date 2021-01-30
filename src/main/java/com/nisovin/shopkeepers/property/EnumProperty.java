package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Property} that stores an enum value.
 *
 * @param <E>
 *            the enum type
 */
public class EnumProperty<E extends Enum<E>> extends Property<E> {

	private final Class<E> enumClass;

	/**
	 * Creates a new {@link EnumProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param enumClass
	 *            the enum's class, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public EnumProperty(AbstractShopkeeper shopkeeper, Class<E> enumClass, String key, E defaultValue) {
		super(shopkeeper, key, defaultValue);
		Validate.notNull(enumClass, "Enum class is null!");
		this.enumClass = enumClass;
	}

	@Override
	protected E loadValue(ConfigurationSection configSection) throws InvalidValueException {
		String valueName = configSection.getString(this.key);
		if (valueName == null) return null;
		E enumValue = EnumUtils.parseEnumValue(enumClass, valueName);
		if (enumValue == null) {
			throw this.invalidValueError(valueName);
		} else {
			return enumValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, E value) {
		configSection.set(this.key, (value == null) ? null : value.name());
	}
}
