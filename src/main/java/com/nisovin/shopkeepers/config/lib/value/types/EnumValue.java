package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class EnumValue<E extends Enum<E>> extends ValueType<E> {

	private static final StringValue STRING_VALUE = new StringValue();

	private final Class<E> enumType;

	public EnumValue(Class<E> enumType) {
		Validate.notNull(enumType, "enumType is null");
		this.enumType = enumType;
	}

	@Override
	public E load(Object configValue) throws ValueLoadException {
		String enumValueName = STRING_VALUE.load(configValue);
		if (enumValueName == null) return null;

		try {
			return this.parse(enumValueName);
		} catch (ValueParseException e) {
			throw this.newInvalidEnumValueException(enumValueName, e);
		}
	}

	protected ValueLoadException newInvalidEnumValueException(String valueName, ValueParseException parseException) {
		return new ValueLoadException(parseException.getMessage(), parseException);
	}

	@Override
	public Object save(E value) {
		if (value == null) return null;
		return value.name();
	}

	@Override
	public String format(E value) {
		if (value == null) return "null";
		return value.name();
	}

	/**
	 * Formats the given String input in a way that matches the names of the enum values.
	 * 
	 * @param input
	 *            the String input, not <code>null</code>
	 * @return the normalized input
	 */
	protected String normalize(String input) {
		assert input != null;
		return StringUtils.normalizeEnumName(input);
	}

	@Override
	public E parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		String normalized = this.normalize(input);
		try {
			return Enum.valueOf(enumType, normalized);
		} catch (IllegalArgumentException e) {
			throw new ValueParseException("Unknown " + enumType.getSimpleName() + ": " + input);
		}
	}
}
