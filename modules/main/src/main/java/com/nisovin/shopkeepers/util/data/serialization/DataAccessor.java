package com.nisovin.shopkeepers.util.data.serialization;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Combines {@link DataSaver} and {@link DataLoader} to provide operations to save and load a
 * particular kind of value to and from a given {@link DataContainer}.
 *
 * @param <T>
 *            the type of the value that is being saved or loaded
 */
public interface DataAccessor<T> extends DataSaver<T>, DataLoader<T> {
}
