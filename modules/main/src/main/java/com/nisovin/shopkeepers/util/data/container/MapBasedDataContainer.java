package com.nisovin.shopkeepers.util.data.container;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataContainer} that is implemented on top of a {@link Map}.
 * <p>
 * This class implements the read-only portion of the {@link Map} interface by delegating to the
 * underlying Map that stores the values. Implementing the Map interface ensures that this
 * {@link DataContainer} is directly serializable by SnakeYaml without requiring additional
 * conversions.
 */
public class MapBasedDataContainer extends AbstractDataContainer {

	private final Map<String, Object> dataMap;
	private @Nullable Set<? extends String> keysView = null; // Lazily set up
	private @Nullable Map<? extends String, @NonNull ?> mapView = null; // Lazily set up

	/**
	 * Creates a new {@link MapBasedDataContainer}.
	 */
	public MapBasedDataContainer() {
		this(new LinkedHashMap<>());
	}

	/**
	 * Creates a new {@link MapBasedDataContainer}.
	 * <p>
	 * The given map is assumed to be mutable (if this data container is supposed to be modifiable)
	 * and only contain valid data container entries.
	 * 
	 * @param dataMap
	 *            the underlying data map, not <code>null</code>
	 */
	public MapBasedDataContainer(Map<String, Object> dataMap) {
		Validate.notNull(dataMap, "dataMap is null");
		this.dataMap = dataMap;
	}

	@Override
	public @Nullable Object getOrDefault(String key, @Nullable Object defaultValue) {
		Validate.notEmpty(key, "key is empty");
		Object value = dataMap.get(key);
		return (value != null) ? value : defaultValue;
	}

	@Override
	public void internalSet(String key, Object value) {
		dataMap.put(key, value);
	}

	@Override
	public void remove(String key) {
		dataMap.remove(key);
	}

	@Override
	public void clear() {
		dataMap.clear();
	}

	@Override
	public int size() {
		return dataMap.size();
	}

	@Override
	public boolean isEmpty() {
		return dataMap.isEmpty();
	}

	@Override
	public Set<? extends String> getKeys() {
		if (keysView == null) {
			keysView = Collections.unmodifiableSet(dataMap.keySet());
		}
		assert keysView != null;
		return keysView;
	}

	@Override
	public Map<? extends String, @NonNull ?> getValues() {
		if (mapView == null) {
			mapView = Collections.unmodifiableMap(dataMap);
		}
		assert mapView != null;
		return mapView;
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return new LinkedHashMap<>(dataMap);
	}

	@Override
	public @Nullable Object serialize() {
		return this.getValues();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MapBasedDataContainer [dataMap=");
		builder.append(dataMap);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return dataMap.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DataContainer)) return false;
		DataContainer otherContainer = (DataContainer) obj;
		return dataMap.equals(otherContainer.getValues());
	}
}
