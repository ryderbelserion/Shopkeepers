package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class EnumValue<E extends Enum<E>> extends ValueType<E> {

	private static final StringValue STRING_VALUE = new StringValue();

	private final Class<@NonNull E> enumType;

	public EnumValue(Class<@NonNull E> enumType) {
		Validate.notNull(enumType, "enumType is null");
		this.enumType = enumType;
	}

	@Override
	public @Nullable E load(@Nullable Object configValue) throws ValueLoadException {
		String enumValueName = STRING_VALUE.load(configValue);
		if (enumValueName == null) return null;

		try {
			return this.parse(enumValueName);
		} catch (ValueParseException e) {
			throw this.newInvalidEnumValueException(enumValueName, e);
		}
	}

	protected ValueLoadException newInvalidEnumValueException(
			String valueName,
			ValueParseException parseException
	) {
		return new ValueLoadException(parseException.getMessage(), parseException);
	}

	@Override
	public @Nullable Object save(@Nullable E value) {
		if (value == null) return null;
		return value.name();
	}

	@Override
	public String format(@Nullable E value) {
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
		return EnumUtils.normalizeEnumName(input);
	}

	@Override
	public @NonNull E parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		String normalized = this.normalize(input);
		try {
			return Enum.valueOf(enumType, normalized);
		} catch (IllegalArgumentException e) {
			throw new ValueParseException("Unknown " + enumType.getSimpleName() + ": " + input);
		}
	}
}
