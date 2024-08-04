package com.nisovin.shopkeepers.util.data.container;

import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.value.DataValue;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataContainer} implementation that dynamically accesses its data source via a given
 * {@link DataValue}.
 * <p>
 * Write operations will dynamically create a new underlying data container if it is missing or if
 * the underlying data is not a valid data container. This expects that the given {@link DataValue}
 * is modifiable, and may overwrite any data that is currently stored for the given
 * {@link DataValue}.
 * <p>
 * Read operations do not attempt to create the underlying data container if it is missing or if the
 * underlying data is not a valid data container, and will instead behave as if this data container
 * is empty. This also applies to {@link Object#hashCode()} and {@link Object#equals(Object)}.
 * <p>
 * {@link #serialize()} returns the data that is currently stored by the given {@link DataValue},
 * even if this data does not represent a valid data container.
 * <p>
 * Implementation note: The underlying data container instance is cached and only recreated if the
 * underlying data source has changed since the last access.
 */
public class DataValueContainer extends AbstractDataContainer {

	/**
	 * Creates a new {@link DataValueContainer} for the given {@link DataValue}.
	 * 
	 * @param dataValue
	 *            the data value that is used to access the underlying data container, or
	 *            <code>null</code>
	 * @return the {@link DataValueContainer}, or <code>null</code> if the given {@link DataValue}
	 *         is <code>null</code>
	 */
	public static @PolyNull DataContainer of(@PolyNull DataValue dataValue) {
		if (dataValue == null) return null;
		return new DataValueContainer(dataValue);
	}

	/**
	 * Creates a new {@link DataValueContainer} for the given {@link DataValue}.
	 * 
	 * @param dataValue
	 *            the data value that is used to access the underlying data container,
	 *            not<code>null</code>
	 * @return the {@link DataValueContainer}, not <code>null</code>
	 */
	public static DataContainer ofNonNull(DataValue dataValue) {
		Validate.notNull(dataValue, "dataValue is null");
		return Unsafe.assertNonNull(of(dataValue));
	}

	/////

	private final DataValue dataValue;

	private @Nullable Object dataSourceCache = null;
	private @Nullable DataContainer dataContainerCache = null;

	/**
	 * Creates a new {@link DataValueContainer}.
	 * 
	 * @param dataValue
	 *            the data value that is used to access the underlying data container, not
	 *            <code>null</code>
	 */
	protected DataValueContainer(DataValue dataValue) {
		Validate.notNull(dataValue, "dataValue is null");
		this.dataValue = dataValue;
	}

	/**
	 * Gets the underlying data container.
	 * <p>
	 * The underlying data container instance is cached and only recreated if the underlying data
	 * source has changed since the last access.
	 * 
	 * @param createIfMissing
	 *            <code>true</code> to create a new underlying data container if it is missing or if
	 *            the underlying data is not a valid data container. This will overwrite any
	 *            currently stored data.
	 * @return the underlying data container, or <code>null</code> if {@code createIfMissing} is
	 *         <code>false</code> and the underlying data is empty or not a valid data container
	 */
	private @Nullable DataContainer getDataContainer(boolean createIfMissing) {
		Object dataSource = dataValue.get();
		if (dataSource != dataSourceCache) {
			// Update the cache:
			dataSourceCache = dataSource;
			dataContainerCache = DataContainer.of(dataSource); // Can be null
		}

		if (createIfMissing && dataContainerCache == null) {
			// Invalid or empty data source.
			// Insert a new empty DataContainer (this replaces any currently stored data):
			dataContainerCache = dataValue.createContainer();
			dataSourceCache = dataValue.get();
		}
		return dataContainerCache; // Can be null
	}

	/**
	 * Gets the underlying data container.
	 * <p>
	 * The underlying data container instance is cached and only recreated if the underlying data
	 * source has changed since the last access.
	 * 
	 * @return the underlying data container, or <code>null</code> if the underlying data is empty
	 *         or not a valid data container
	 */
	protected final @Nullable DataContainer getDataContainer() {
		return this.getDataContainer(false);
	}

	/**
	 * Gets the underlying data container.
	 * <p>
	 * The underlying data container instance is cached and only recreated if the underlying data
	 * source has changed since the last access.
	 * <p>
	 * If the underlying data is empty or not a valid data container, this returns
	 * {@link DataContainer#EMPTY}.
	 * 
	 * @return the underlying data container, or {@link DataContainer#EMPTY} if the underlying data
	 *         is empty or not a valid data container, not <code>null</code>
	 */
	protected final DataContainer getDataContainerOrEmpty() {
		DataContainer dataContainer = this.getDataContainer(false);
		if (dataContainer != null) {
			return dataContainer;
		} else {
			return DataContainer.EMPTY;
		}
	}

	/**
	 * Gets the underlying data container.
	 * <p>
	 * If the underlying data is empty or not a valid data container, this will create and insert a
	 * new data container, and thereby overwrite any currently stored data for the {@link DataValue}
	 * of this {@link DataValueContainer}.
	 * <p>
	 * The underlying data container instance is cached and only recreated if the underlying data
	 * source has changed since the last access.
	 * 
	 * @return the underlying data container, not <code>null</code>
	 */
	protected final DataContainer getOrCreateDataContainer() {
		DataContainer dataContainer = this.getDataContainer(true);
		return Unsafe.assertNonNull(dataContainer);
	}

	@Override
	public @Nullable Object getOrDefault(String key, @Nullable Object defaultValue) {
		return this.getDataContainerOrEmpty().getOrDefault(key, defaultValue);
	}

	@Override
	public void set(String key, @Nullable Object value) {
		this.getOrCreateDataContainer().set(key, value);
	}

	@Override
	protected void internalSet(String key, Object value) {
		// Not expected to be called, because we override #set(String, Object).
		throw new IllegalStateException("This method is not expected to be called!");
	}

	@Override
	public void remove(String key) {
		this.getOrCreateDataContainer().remove(key);
	}

	@Override
	public void clear() {
		this.getOrCreateDataContainer().clear();
	}

	@Override
	public int size() {
		return this.getDataContainerOrEmpty().size();
	}

	@Override
	public Set<? extends String> getKeys() {
		return this.getDataContainerOrEmpty().getKeys();
	}

	@Override
	public Map<? extends String, @NonNull ?> getValues() {
		return this.getDataContainerOrEmpty().getValues();
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return this.getDataContainerOrEmpty().getValuesCopy();
	}

	@Override
	public @Nullable Object serialize() {
		// Retain the data that is currently stored, even if it is not a valid data container:
		return dataValue.get();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [dataValue=");
		builder.append(dataValue);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return this.getDataContainerOrEmpty().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) return true;
		return this.getDataContainerOrEmpty().equals(obj);
	}
}
