package com.nisovin.shopkeepers.util.data.container;

import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for {@link DataContainer} implementations that delegate to another data container.
 */
public class DelegateDataContainer extends AbstractDataContainer {

	protected final DataContainer dataContainer;

	/**
	 * Creates a new {@link DelegateDataContainer}.
	 * 
	 * @param dataContainer
	 *            the underlying data container, not <code>null</code>
	 */
	public DelegateDataContainer(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		this.dataContainer = dataContainer;
	}

	@Override
	public @Nullable Object getOrDefault(String key, @Nullable Object defaultValue) {
		return dataContainer.getOrDefault(key, defaultValue);
	}

	@Override
	public void set(String key, @Nullable Object value) {
		dataContainer.set(key, value);
	}

	@Override
	protected void internalSet(String key, Object value) {
		// Not expected to be called, because we override #set(String, Object).
		throw new IllegalStateException("This method is not expected to be called!");
	}

	@Override
	public void remove(String key) {
		dataContainer.remove(key);
	}

	@Override
	public void clear() {
		dataContainer.clear();
	}

	@Override
	public int size() {
		return dataContainer.size();
	}

	@Override
	public Set<? extends String> getKeys() {
		return dataContainer.getKeys();
	}

	@Override
	public Map<? extends String, @NonNull ?> getValues() {
		return dataContainer.getValues();
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return dataContainer.getValuesCopy();
	}

	@Override
	public @Nullable Object serialize() {
		return dataContainer.serialize();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [dataContainer=");
		builder.append(dataContainer);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return dataContainer.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return dataContainer.equals(obj);
	}
}
