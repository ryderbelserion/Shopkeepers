package com.nisovin.shopkeepers.util.data.property.validation;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;

/**
 * Validates the values of a {@link Property}.
 * <p>
 * {@link PropertyValidator}s are only invoked for non-<code>null</code> values, because
 * <code>null</code> values are already handled separately by the {@link Property} itself.
 *
 * @param <T>
 *            the type of the validated value
 * @see BasicProperty#validator(PropertyValidator)
 */
@FunctionalInterface
public interface PropertyValidator<@NonNull T> {

	/**
	 * Validates the given non-<code>null</code> value.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given value is invalid
	 */
	public void validate(@NonNull T value);
}
