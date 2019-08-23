package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.Validate;

public class EnumProperty<E extends Enum<E>> extends Property<E> {

	private final Class<E> enumClass;

	public EnumProperty(Class<E> enumClass, String key, E defaultValue) {
		super(key, defaultValue);
		Validate.notNull(enumClass, "Enum class is null!");
		this.enumClass = enumClass;
	}

	@Override
	protected E loadValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection) throws InvalidValueException {
		String valueName = configSection.getString(this.key);
		if (valueName == null) return null;
		E enumValue = EnumUtils.parseEnumValue(enumClass, valueName);
		if (enumValue == null) {
			throw this.invalidValueError(shopkeeper, valueName);
		} else {
			return enumValue;
		}
	}

	@Override
	protected void saveValue(AbstractShopkeeper shopkeeper, ConfigurationSection configSection, E value) {
		configSection.set(this.key, value == null ? null : value.name());
	}
}
