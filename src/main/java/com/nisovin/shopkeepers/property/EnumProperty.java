package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Property} that stores an enum value.
 *
 * @param <E>
 *            the enum type
 */
public class EnumProperty<E extends Enum<E>> extends Property<E> {

	private final Class<E> enumType;

	/**
	 * Creates a new {@link EnumProperty}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param enumType
	 *            the enum's class, not <code>null</code>
	 * @param key
	 *            the storage key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 */
	public EnumProperty(AbstractShopkeeper shopkeeper, Class<E> enumType, String key, E defaultValue) {
		super(shopkeeper, key, defaultValue);
		Validate.notNull(enumType, "enumType is null");
		this.enumType = enumType;
	}

	@Override
	protected E loadValue(ConfigurationSection configSection) throws InvalidValueException {
		String valueName = configSection.getString(key);
		if (valueName == null) return null;
		E enumValue = ConversionUtils.parseEnum(enumType, valueName);
		if (enumValue == null) {
			throw this.invalidValueError(valueName);
		} else {
			return enumValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, E value) {
		configSection.set(key, (value == null) ? null : value.name());
	}
}
