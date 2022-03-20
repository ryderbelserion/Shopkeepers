package com.nisovin.shopkeepers.util.data.serialization;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Loads a particular kind of value from a given {@link DataContainer}.
 * <p>
 * Unlike {@link DataSerializer}, which converts values of a certain type to and from a single
 * serializable data object, a {@link DataLoader} has access to a whole {@link DataContainer} from
 * which it may extract one or multiple serializable data elements to reconstruct its value.
 * <p>
 * {@link DataLoader}s are sometimes used to load values with domain-specific semantics. Depending
 * on their concrete implementation, they may handle additional higher level aspects, such as the
 * validation of loaded values, or the fallback to predefined default values when data is missing or
 * invalid. However, the exact loading behavior is undefined at the level of this interface and is
 * instead specified by concrete implementations.
 * <p>
 * Since the reconstruction of values is dependent on how the value was originally serialized,
 * {@link DataLoader} is usually not implemented on its own, but in combination with a corresponding
 * {@link DataSaver} in the form of a {@link DataAccessor}.
 *
 * @param <T>
 *            the type of the value that is being loaded
 */
public interface DataLoader<T> {

	/**
	 * Reconstructs the value from the data inside the given {@link DataContainer}.
	 * <p>
	 * It is undefined how this method reacts to missing or invalid data: Some implementations may
	 * throw a corresponding {@link InvalidDataException}, whereas others may return some fallback
	 * value in this case.
	 * 
	 * @param dataContainer
	 *            the data container not <code>null</code>
	 * @return the loaded value, can be <code>null</code> (depending on how this {@link DataLoader}
	 *         reacts to missing or invalid data)
	 * @throws MissingDataException
	 *             if the data for the value is missing (optional)
	 * @throws InvalidDataException
	 *             if the value cannot be loaded or is invalid (optional)
	 */
	public T load(DataContainer dataContainer) throws InvalidDataException;
}
