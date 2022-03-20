package com.nisovin.shopkeepers.util.data.property;

import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataAccessor;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.java.PredicateUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataAccessor} that uses a {@link DataSerializer} to save and load its value from the
 * data at a specific data key.
 *
 * @param <T>
 *            the type of the value that is being saved or loaded
 */
public class DataKeyAccessor<T> implements DataAccessor<@NonNull T> {

	private final String dataKey;
	private final DataSerializer<@NonNull T> serializer;
	private Predicate<Object> emptyDataPredicate = PredicateUtils.alwaysFalse();

	/**
	 * Creates a new {@link DataKeyAccessor}.
	 * 
	 * @param dataKey
	 *            the data key, not <code>null</code> or empty
	 * @param serializer
	 *            the data serializer, not <code>null</code>
	 */
	public DataKeyAccessor(String dataKey, DataSerializer<@NonNull T> serializer) {
		Validate.notEmpty(dataKey, "dataKey is null or empty");
		Validate.notNull(serializer, "serializer is null");
		// TODO Validate that the key is valid for use as data key?
		this.dataKey = dataKey;
		this.serializer = serializer;
	}

	/**
	 * Gets the data key.
	 * 
	 * @return the data key, not <code>null</code> or empty
	 */
	public final String getDataKey() {
		return dataKey;
	}

	/**
	 * Gets the {@link DataSerializer}.
	 * 
	 * @return the data serializer, not <code>null</code>
	 */
	public final DataSerializer<@NonNull T> getSerializer() {
		return serializer;
	}

	/**
	 * Sets the {@link Predicate} that decides if the data accessed by this {@link DataKeyAccessor}
	 * shall be considered empty and therefore treated as if it is missing.
	 * <p>
	 * The predicate is only evaluated for non-<code>null</code> data: <code>null</code> is always
	 * considered missing data.
	 * <p>
	 * Emptiness is usually defined for a specific data type, but the predicate can be evaluated for
	 * data of arbitrary type. The predicate is expected to evaluate to <code>false</code> for any
	 * data that does not match its emptiness definition.
	 * 
	 * @param <A>
	 *            the type of this data accessor
	 * @param emptyDataPredicate
	 *            the predicate, not <code>null</code>
	 * @return this data accessor
	 */
	@SuppressWarnings("unchecked")
	public final <A extends DataKeyAccessor<T>> A emptyDataPredicate(
			Predicate<Object> emptyDataPredicate
	) {
		Validate.notNull(emptyDataPredicate, "emptyDataPredicate is null");
		this.emptyDataPredicate = emptyDataPredicate;
		return (A) this;
	}

	@Override
	public final void save(DataContainer dataContainer, @Nullable T value) {
		Validate.notNull(dataContainer, "dataContainer is null");
		if (value == null) {
			this.setData(dataContainer, null);
		} else {
			Object serialized = serializer.serialize(value);
			this.setData(dataContainer, serialized);
		}
	}

	/**
	 * Uses the {@link #getSerializer() DataSerializer} to reconstruct the value from the data
	 * stored in the given {@link DataContainer} at the {@link #getDataKey() data key} of this
	 * {@link DataKeyAccessor}.
	 * 
	 * @param dataContainer
	 *            the data container not <code>null</code>
	 * @return the loaded value, not <code>null</code>
	 * @throws MissingDataException
	 *             if the data for the value is missing, or satisfies the
	 *             {@link #emptyDataPredicate(Predicate) empty data predicate}
	 * @throws InvalidDataException
	 *             if the value cannot be deserialized
	 */
	@Override
	public final @NonNull T load(DataContainer dataContainer) throws InvalidDataException {
		Validate.notNull(dataContainer, "dataContainer is null");
		Object data = this.getData(dataContainer);
		if (data == null || emptyDataPredicate.test(data)) {
			throw this.missingDataError();
		} else {
			T value;
			try {
				// This is expected to throw an exception if it cannot reconstruct the original
				// value:
				value = serializer.deserialize(data);
			} catch (MissingDataException e) {
				throw new RuntimeException("Serializer of type " + serializer.getClass().getName()
						+ " threw an unexpected " + MissingDataException.class.getName()
						+ " during deserialization: " + data, e);
			}
			if (Unsafe.cast(value) == null) {
				throw new RuntimeException("Serializer of type " + this.getClass().getName()
						+ " deserialized data to null: " + data);
			}
			return value;
		}
	}

	/**
	 * Creates a {@link MissingDataException} that is used when the data for this
	 * {@link DataKeyAccessor} is missing.
	 * 
	 * @return the {@link MissingDataException}
	 */
	protected MissingDataException missingDataError() {
		return new MissingDataException("Missing data.");
	}

	// DIRECT DATA ACCESS

	/**
	 * Gets the raw data that is stored in the given data container at this data accessor's
	 * {@link #getDataKey() data key}.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @return the data, or <code>null</code> if there is no data
	 */
	private final @Nullable Object getData(DataContainer dataContainer) {
		assert dataContainer != null;
		return dataContainer.get(dataKey);
	}

	/**
	 * Sets the raw data in the given data container at this data accessor's {@link #getDataKey()
	 * data key}.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param data
	 *            the data, or <code>null</code> to clear the data
	 */
	private final void setData(DataContainer dataContainer, @Nullable Object data) {
		assert dataContainer != null;
		dataContainer.set(dataKey, data);
	}
}
