package com.nisovin.shopkeepers.util.data;

import java.util.Map;
import java.util.Set;

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
	public Object getOrDefault(String key, Object defaultValue) {
		return dataContainer.getOrDefault(key, defaultValue);
	}

	@Override
	public void set(String key, Object value) {
		dataContainer.set(key, value);
	}

	@Override
	protected void setInternal(String key, Object value) {
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
	public Set<String> getKeys() {
		return dataContainer.getKeys();
	}

	@Override
	public Map<String, ?> getValues() {
		return dataContainer.getValues();
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return dataContainer.getValuesCopy();
	}

	@Override
	public Object serialize() {
		return dataContainer.serialize();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DelegateDataContainer [dataContainer=");
		builder.append(dataContainer);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return dataContainer.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return dataContainer.equals(obj);
	}
}
