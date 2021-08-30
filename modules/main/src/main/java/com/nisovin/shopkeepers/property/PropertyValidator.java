package com.nisovin.shopkeepers.property;

/**
 * Validates the values of a {@link Property}.
 *
 * @param <T>
 *            the type of the validated values
 */
@FunctionalInterface
public interface PropertyValidator<T> {

	/**
	 * Validates the given value.
	 * 
	 * @param property
	 *            the involved property, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is considered invalid
	 */
	public void validate(Property<T> property, T value);
}
