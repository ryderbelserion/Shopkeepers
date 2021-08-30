package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

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
	 * @param enumType
	 *            the enum's class, not <code>null</code>
	 */
	public EnumProperty(Class<E> enumType) {
		Validate.notNull(enumType, "enumType is null");
		this.enumType = enumType;
	}

	@Override
	protected E loadValue(ConfigurationSection configSection) throws InvalidValueException {
		String valueName = configSection.getString(this.getKey());
		if (valueName == null) return null;
		E enumValue = ConversionUtils.parseEnum(enumType, valueName);
		if (enumValue == null) {
			throw new InvalidValueException("Failed to parse " + enumType.getSimpleName() + ": '" + valueName + "'.");
		} else {
			return enumValue;
		}
	}

	@Override
	protected void saveValue(ConfigurationSection configSection, E value) {
		configSection.set(this.getKey(), (value == null) ? null : value.name());
	}
}
