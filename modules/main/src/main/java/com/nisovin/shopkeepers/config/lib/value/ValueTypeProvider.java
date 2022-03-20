package com.nisovin.shopkeepers.config.lib.value;

import java.lang.reflect.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ValueTypeProvider {

	/**
	 * Gets the {@link ValueType} for the given setting type, if it can provide one.
	 * <p>
	 * The returned {@link ValueType} may get cached and used for future requests for the same
	 * setting type. The provider is therefore only allowed to take the type itself into account,
	 * and not any other contextual state.
	 * 
	 * @param type
	 *            the type of the setting's value
	 * @return the value type, or <code>null</code> if none can be provided
	 */
	public @Nullable ValueType<?> get(Type type);
}
