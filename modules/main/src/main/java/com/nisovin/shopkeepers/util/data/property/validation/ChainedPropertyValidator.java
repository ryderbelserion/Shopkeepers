package com.nisovin.shopkeepers.util.data.property.validation;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link PropertyValidator} that invokes two other validators.
 * <p>
 * This can be used to chain multiple property validators together.
 *
 * @param <T>
 *            the type of the validated value
 */
public final class ChainedPropertyValidator<T> implements PropertyValidator<@NonNull T> {

	private final PropertyValidator<? super @NonNull T> first;
	private final PropertyValidator<? super @NonNull T> second;

	/**
	 * Creates a new {@link ChainedPropertyValidator}.
	 * 
	 * @param first
	 *            the first validator, not <code>null</code>
	 * @param second
	 *            the second validator, not <code>null</code>
	 */
	public ChainedPropertyValidator(
			PropertyValidator<? super @NonNull T> first,
			PropertyValidator<? super @NonNull T> second
	) {
		Validate.notNull(first, "first is null");
		Validate.notNull(second, "second is null");
		this.first = first;
		this.second = second;
	}

	@Override
	public void validate(@NonNull T value) {
		first.validate(value);
		second.validate(value);
	}
}
