package com.nisovin.shopkeepers.util.data;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;

/**
 * Provides operations to save and load a particular kind of value to and from a given {@link DataContainer}.
 * <p>
 * Unlike {@link DataSerializer}, which converts values of a certain type to and from a single serializable data object,
 * a {@link DataAccessor} has access to a whole {@link DataContainer} in which it may embed or extract one or multiple
 * data elements to represent or reconstruct its value.
 * <p>
 * {@link DataAccessor}s are sometimes used to save and load values with domain-specific semantics. Depending on the
 * concrete implementation, their saving and loading methods may handle additional higher level aspects, such as the
 * validation of saved and loaded values, or the fallback to predefined default values when the data for the value is
 * missing or invalid. However, the exact saving and loading behavior is undefined at the level of this interface and is
 * instead specified by concrete implementations.
 *
 * @param <T>
 *            the type of the value that is being saved or loaded
 */
public interface DataAccessor<T> {

	/**
	 * Serializes the given value to the given {@link DataContainer}.
	 * <p>
	 * Depending on the implementation of this {@link DataAccessor}, this might first validate the given value.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is invalid (optional)
	 */
	public void save(DataContainer dataContainer, T value);

	/**
	 * Reconstructs the value from the data inside the given {@link DataContainer}.
	 * <p>
	 * It is undefined how this methods reacts to missing or invalid data: Some implementations may throw a
	 * corresponding {@link InvalidDataException}, whereas others may return some fallback value in this case.
	 * 
	 * @param dataContainer
	 *            the data container not <code>null</code>
	 * @return the loaded value, can be <code>null</code> (depending on how this accessor reacts to missing or invalid
	 *         data)
	 * @throws MissingDataException
	 *             if the data for the value is missing (optional)
	 * @throws InvalidDataException
	 *             if the value cannot be loaded or is invalid (optional)
	 */
	public T load(DataContainer dataContainer) throws InvalidDataException;
}
