package com.nisovin.shopkeepers.config.lib.value;

import com.nisovin.shopkeepers.config.lib.annotation.WithDefaultValueType;
import com.nisovin.shopkeepers.config.lib.annotation.WithValueType;
import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Defines how values of this type are loaded from and saved to configs.
 * <p>
 * Subtypes should ideally provide a public no-args constructor so that they can be used together with the
 * {@link WithValueType} and {@link WithDefaultValueType} annotations.
 *
 * @param <T>
 *            the type of value
 */
public abstract class ValueType<T> {

	public ValueType() {
	}

	// LOAD

	public T load(DataContainer dataContainer, String key) throws ValueLoadException {
		Object configValue = dataContainer.get(key);
		return this.load(configValue);
	}

	public T load(DataContainer dataContainer, String key, T defaultValue) {
		T value = null;
		try {
			value = this.load(dataContainer, key);
		} catch (ValueLoadException e) {
		}
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	// Null indicates the absence of a value and should only be used if the input has been null.
	public abstract T load(Object configValue) throws ValueLoadException;

	// SAVE

	// A value of null will clear the data entry.
	public void save(DataContainer dataContainer, String key, T value) {
		Object configValue = this.save(value); // Can be null
		dataContainer.set(key, configValue);
	}

	public abstract Object save(T value);

	// OTHER (OPTIONAL) UTILITIES

	/**
	 * Formats the given value to a (user-friendly) String.
	 * <p>
	 * By default this calls {@link String#valueOf(Object)} for the given value. Override this to produce more
	 * user-friendly Strings if possible.
	 * 
	 * @param value
	 *            the value
	 * @return the formatted value, not <code>null</code>
	 */
	public String format(T value) {
		return String.valueOf(value);
	}

	/**
	 * Tries to parse the value from the given String.
	 * <p>
	 * Override this if parsing from String is supported.
	 * 
	 * @param input
	 *            the input string, not <code>null</code>
	 * @return the parsed value, not <code>null</code>
	 * @throws UnsupportedOperationException
	 *             if parsing of values is not supported
	 * @throws ValueParseException
	 *             if the value could not be parsed
	 */
	public T parse(String input) throws ValueParseException {
		throw new UnsupportedOperationException("This ValueType does not support the parsing of values.");
	}
}
