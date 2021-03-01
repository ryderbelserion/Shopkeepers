package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class EnumValue<E extends Enum<E>> extends ValueType<E> {

	private static final StringValue STRING_VALUE = new StringValue();

	private final Class<E> enumClass;

	public EnumValue(Class<E> enumClass) {
		Validate.notNull(enumClass, "enumClass is null");
		this.enumClass = enumClass;
	}

	@Override
	public E load(Object configValue) throws ValueLoadException {
		String enumValueName = STRING_VALUE.load(configValue);
		if (enumValueName == null) return null;

		try {
			return this.parse(enumValueName);
		} catch (ValueParseException e) {
			throw this.newUnknownEnumValueException(enumValueName, e);
		}
	}

	protected ValueLoadException newUnknownEnumValueException(String valueName, ValueParseException parseException) {
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

	@Override
	public E parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");

		String normalized = StringUtils.normalizeEnumName(input);
		try {
			return Enum.valueOf(enumClass, normalized);
		} catch (IllegalArgumentException e) {
			throw new ValueParseException("Unknown " + enumClass.getSimpleName() + ": " + input);
		}
	}
}
