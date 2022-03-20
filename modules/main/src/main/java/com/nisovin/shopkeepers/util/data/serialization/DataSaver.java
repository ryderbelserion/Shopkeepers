package com.nisovin.shopkeepers.util.data.serialization;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Saves a particular kind of value to a given {@link DataContainer}.
 * <p>
 * Unlike {@link DataSerializer}, which converts values of a certain type to and from a single
 * serializable data object, a {@link DataSaver} has access to a whole {@link DataContainer} in
 * which it may embed one or multiple serializable data elements to represent its value.
 * <p>
 * {@link DataSaver}s are sometimes used to save values with domain-specific semantics. Depending on
 * their concrete implementation, they may handle additional higher level aspects, such as the
 * validation of saved values. However, the exact saving behavior is undefined at the level of this
 * interface and is instead specified by concrete implementations.
 * <p>
 * Since the later reconstruction of values is dependent on how the value was originally serialized,
 * {@link DataSaver} is usually not implemented on its own, but in combination with a corresponding
 * {@link DataLoader} in the form of a {@link DataAccessor}.
 *
 * @param <T>
 *            the type of the value that is being saved
 */
public interface DataSaver<T> {

	/**
	 * Serializes the given value to the given {@link DataContainer}.
	 * <p>
	 * Depending on the implementation of this {@link DataSaver}, this might first validate the
	 * given value.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is invalid (optional)
	 */
	public void save(DataContainer dataContainer, @Nullable T value);
}
