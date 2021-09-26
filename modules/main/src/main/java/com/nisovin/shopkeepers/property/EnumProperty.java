package com.nisovin.shopkeepers.property;

import com.nisovin.shopkeepers.util.data.InvalidDataException;
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
	protected E deserializeValue(Object dataObject) throws InvalidDataException {
		assert dataObject != null;
		if (!(dataObject instanceof String)) {
			throw new InvalidDataException(enumType.getSimpleName() + " data is not of type String, but "
					+ dataObject.getClass().getName() + ".");
		}

		String valueName = (String) dataObject;
		E value = ConversionUtils.parseEnum(enumType, valueName);
		if (value == null) {
			throw new InvalidDataException("Failed to parse " + enumType.getSimpleName() + " from '" + valueName + "'.");
		} else {
			return value;
		}
	}

	@Override
	protected Object serializeValue(E value) {
		return value.name();
	}
}
