package com.nisovin.shopkeepers.util.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

/**
 * A container of data elements that are indexed by {@link String} keys.
 * <p>
 * This interface provides a unified view on data that may be stored in different underlying data structures, such as
 * String-Object {@link Map}s or {@link ConfigurationSection}s. With the static method {@link #of(Object)} or the member
 * {@link #getContainer(String)}, a supported data source, such as one of the just mentioned, can be turned into a
 * {@link DataContainer}. The returned data container reads and writes through to the underlying data source. The static
 * factory method {@link #create()} is a shortcut to create a new Map-based data container.
 * <p>
 * One use of {@link DataContainer} is to serve as an intermediate representation of data, independent of any concrete
 * storage format. In order to simplify the serialization of data containers to different storage formats, serializers
 * that represent the state of an object as a data container or as a value inside a data container must limit themselves
 * to the following supported types of values:
 * <ul>
 * <li>{@link String}s, {@link Number}s, {@link Boolean}s.
 * <li>String-Object {@link Map}s that only contain values of the here mentioned types.
 * <li>{@link ConfigurationSerializable}s that {@link ConfigurationSerializable#serialize() serialize} their state to a
 * map that only contains values of the here mentioned types.
 * <li>The {@link #serialize() serialized forms} of other {@link DataContainer}s. Directly serializing data containers
 * themselves might not be supported. The original data containers can be reconstructed via {@link #of(Object)} or
 * {@link #getContainer(String)}.
 * <li>{@link List}s of the here mentioned data types.
 * </ul>
 * More complex objects need to be broken down into these supported primitives and reconstructed after deserialization.
 * <p>
 * However, concrete storage formats usually support additional data types. {@link DataContainer} is supposed to be able
 * to represent any Map-like data that was read from some storage format, without enforcing the conversion or omission
 * of unsupported types of values, or constraining the supported input. Consequently, a data container whose contents
 * were read from some storage format might not be serializable in some other storage format if it contains unsupported
 * types of values. And deserializers need to be aware that they may encounter unexpected types of values when they try
 * to reconstruct some type of object from a data container of unknown source.
 * <p>
 * Another aspect of the goal of serving as an intermediate representation for different storage formats is that certain
 * properties of the stored data are not guaranteed to be preserved across serialization and deserialization:
 * <ul>
 * <li>Exact data types might not be preserved. I.e. the representation and implementation type of numbers, lists, maps,
 * and {@link DataContainer}s might change. Any method that retrieves a value with a specific type will try to convert
 * the stored value to the requested type.
 * <li>Object identities and object references might not be preserved. For example, if a list contains the same object
 * multiple times, these objects might end up being separate independent objects after deserialization.
 * </ul>
 * <p>
 * {@link Object#equals(Object)} compares data containers based on their stored contents. This operation might be
 * relatively costly for some data container implementations.
 */
public interface DataContainer {

	/**
	 * Creates a new empty {@link DataContainer}.
	 * 
	 * @return the new data container
	 */
	public static DataContainer create() {
		return new MapBasedDataContainer();
	}

	/**
	 * Creates a new {@link DataContainer} for the given data source.
	 * <p>
	 * The currently supported types of data sources are:
	 * <ul>
	 * <li>{@link DataContainer} (returns the given data container itself).
	 * <li>{@link Map} (the given map is assumed to be a String-Object map).
	 * <li>{@link ConfigurationSection}.
	 * </ul>
	 * For any other type of object this returns <code>null</code>.
	 * <p>
	 * The returned data container reads and writes through to the given data source. If the given data source is
	 * unmodifiable, the returned data container won't be modifiable either.
	 * 
	 * @param dataSource
	 *            the data source, can be <code>null</code>
	 * @return the data container, or <code>null</code> if the given object is not a valid type of data source
	 */
	public static DataContainer of(Object dataSource) {
		if (dataSource == null) return null;
		if (dataSource instanceof ConfigurationSection) {
			ConfigurationSection configSection = (ConfigurationSection) dataSource;
			return new ConfigBasedDataContainer(configSection);
		} else if (dataSource instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) dataSource;
			return new MapBasedDataContainer(map);
		} else if (dataSource instanceof DataContainer) {
			return (DataContainer) dataSource;
		} else {
			return null;
		}
	}

	/**
	 * Checks if the given object is not <code>null</code> and of a type that can be {@link #of(Object) converted} to a
	 * {@link DataContainer}.
	 * 
	 * @param dataSource
	 *            the object to check, can be <code>null</code>
	 * @return <code>true</code> if the object is not <code>null</code> and of a type that can be converted to a data
	 *         container
	 */
	public static boolean isDataContainer(Object dataSource) {
		if (dataSource == null) return false;
		return dataSource instanceof ConfigurationSection
				|| dataSource instanceof Map
				|| dataSource instanceof DataContainer;
	}

	/////

	/**
	 * Checks if this data container stores a value for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if this data container stores a value for the given key
	 */
	public default boolean contains(String key) {
		return (this.get(key) != null);
	}

	/**
	 * Gets the value for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the value, or <code>null</code> if there is no value for the given key
	 */
	public default Object get(String key) {
		return this.get(key, null);
	}

	/**
	 * Gets the value for the given key.
	 * <p>
	 * If there is no value for the given key, this returns the specified default value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the value, or the given default value if there is no value for the given key
	 */
	public Object get(String key, Object defaultValue);

	/**
	 * Checks if the value for the given key is present and of the specified type.
	 * <p>
	 * This does not check if the stored value can be converted to the specified type.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param type
	 *            the expected type of the stored value
	 * @return <code>true</code> if the value is present and of the specified type
	 */
	public boolean isType(String key, Class<?> type);

	/**
	 * Gets the value of the specified type for the given key.
	 * 
	 * @param <T>
	 *            the expected type of the stored value
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param type
	 *            the expected type of the stored value
	 * @return the value, or <code>null</code> if there is no value of the specified type for the given key
	 */
	public default <T> T getTyped(String key, Class<T> type) {
		return this.getTyped(key, type, null);
	}

	/**
	 * Gets the value of the specified type for the given key.
	 * <p>
	 * If there is no value of the specified type for the given key, this returns the specified default value.
	 * 
	 * @param <T>
	 *            the expected type of the stored value
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param type
	 *            the expected type of the stored value
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the value, or the given default value if there is no value of the specified type for the given key
	 */
	public <T> T getTyped(String key, Class<T> type, T defaultValue);

	/**
	 * Checks if the value for the given key is present and a {@link String}.
	 * <p>
	 * This does not check if the stored value can be converted to a String.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if the value is present and a String
	 */
	public default boolean isString(String key) {
		Object value = this.get(key);
		return (value instanceof String);
	}

	/**
	 * Gets the value for the given key converted to a {@link String}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the String, or <code>null</code> if the value cannot be converted to a String
	 */
	public default String getString(String key) {
		return this.getString(key, null);
	}

	/**
	 * Gets the value for the given key converted to a {@link String}.
	 * <p>
	 * If there is no value for the given key that can be converted to a String, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the String, or the given default value if the value cannot be converted to a String
	 */
	public String getString(String key, String defaultValue);

	/**
	 * Checks if the value for the given key is present and a {@link Number}.
	 * <p>
	 * This does not check if the stored value can be converted to a Number.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if the value is present and a Number
	 */
	public default boolean isNumber(String key) {
		Object value = this.get(key);
		return (value instanceof Number);
	}

	/**
	 * Gets the value for the given key converted to an {@link Integer}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the Integer (not <code>null</code>), or <code>0</code> if the value cannot be converted to an Integer
	 */
	public default int getInt(String key) {
		return this.getInt(key, 0);
	}

	/**
	 * Gets the value for the given key converted to an {@link Integer}.
	 * <p>
	 * If there is no value for the given key that can be converted to an Integer, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 * @return the Integer (not <code>null</code>), or the given default value if the value cannot be converted to an
	 *         Integer
	 */
	public int getInt(String key, int defaultValue);

	/**
	 * Gets the value for the given key converted to a {@link Long}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the Long (not <code>null</code>), or <code>0L</code> if the value cannot be converted to a Long
	 */
	public default long getLong(String key) {
		return this.getLong(key, 0L);
	}

	/**
	 * Gets the value for the given key converted to a {@link Long}.
	 * <p>
	 * If there is no value for the given key that can be converted to a Long, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 * @return the Long (not <code>null</code>), or the given default value if the value cannot be converted to a Long
	 */
	public long getLong(String key, long defaultValue);

	/**
	 * Gets the value for the given key converted to a {@link Float}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the Float (not <code>null</code>), or <code>0.0F</code> if the value cannot be converted to a Float
	 */
	public default float getFloat(String key) {
		return this.getFloat(key, 0.0F);
	}

	/**
	 * Gets the value for the given key converted to a {@link Float}.
	 * <p>
	 * If there is no value for the given key that can be converted to a Float, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 * @return the Float (not <code>null</code>), or the given default value if the value cannot be converted to a Float
	 */
	public float getFloat(String key, float defaultValue);

	/**
	 * Gets the value for the given key converted to a {@link Double}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the Double (not <code>null</code>), or <code>0.0D</code> if the value cannot be converted to a Double
	 */
	public default double getDouble(String key) {
		return this.getDouble(key, 0.0D);
	}

	/**
	 * Gets the value for the given key converted to a {@link Double}.
	 * <p>
	 * If there is no value for the given key that can be converted to a Double, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 * @return the Double (not <code>null</code>), or the given default value if the value cannot be converted to a
	 *         Double
	 */
	public double getDouble(String key, double defaultValue);

	/**
	 * Checks if the value for the given key is present and a {@link Boolean}.
	 * <p>
	 * This does not check if the stored value can be converted to a Boolean.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if the value is present and a Boolean
	 */
	public default boolean isBoolean(String key) {
		Object value = this.get(key);
		return (value instanceof Boolean);
	}

	/**
	 * Gets the value for the given key converted to a {@link Boolean}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the Boolean (not <code>null</code>), or <code>false</code> if the value cannot be converted to a Boolean
	 */
	public default boolean getBoolean(String key) {
		return this.getBoolean(key, false);
	}

	/**
	 * Gets the value for the given key converted to a {@link Boolean}.
	 * <p>
	 * If there is no value for the given key that can be converted to a Boolean, this returns the specified default
	 * value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value
	 * @return the Boolean (not <code>null</code>), or the given default value if the value cannot be converted to a
	 *         Boolean
	 */
	public boolean getBoolean(String key, boolean defaultValue);

	/**
	 * Checks if the value for the given key is present and a {@link List}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if the value is present and a List
	 */
	public default boolean isList(String key) {
		Object value = this.get(key);
		return (value instanceof List);
	}

	/**
	 * Gets the {@link List} for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the list, or <code>null</code> if there is no list for the given key
	 */
	public default List<?> getList(String key) {
		return this.getList(key, null);
	}

	/**
	 * Gets the {@link List} for the given key.
	 * <p>
	 * If there is no list for the given key, this returns the specified default value.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the list, or the given default value if there is no list for the given key
	 */
	public default List<?> getList(String key, List<?> defaultValue) {
		Object value = this.get(key);
		if (value instanceof List) {
			return (List<?>) value;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks if the value for the given key is present and can be {@link #of(Object) converted} to a
	 * {@link DataContainer}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return <code>true</code> if the value is present and can be converted to a data container
	 */
	public default boolean isContainer(String key) {
		Object value = this.get(key);
		return DataContainer.isDataContainer(value);
	}

	/**
	 * Gets the value for the given key {@link #of(Object) converted} to a {@link DataContainer}.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the data container, or <code>null</code> if the value cannot be converted to a data container
	 */
	public default DataContainer getContainer(String key) {
		Object value = this.get(key);
		return DataContainer.of(value);
	}

	/**
	 * Creates a new empty {@link DataContainer} and stores its {@link #serialize() serialized form} for the given key.
	 * <p>
	 * The returned data container reads and writes through to its stored serialized form.
	 * <p>
	 * This will replace any other value that may currently be stored for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @return the newly created data container
	 * @throws UnsupportedOperationException
	 *             if this data container is unmodifiable
	 */
	public default DataContainer createContainer(String key) {
		DataContainer container = DataContainer.create();
		this.set(key, container.serialize());
		return container;
	}

	/**
	 * Sets the value for the given key.
	 * <p>
	 * This will replace any other value that may currently be stored for the given key.
	 * <p>
	 * If the given value is <code>null</code>, this will remove the currently stored value for the given key by
	 * delegating the method call to {@link #remove(String)}.
	 * <p>
	 * Because {@link DataContainer}s themselves are not guaranteed to be serializable and inserting a data container
	 * instead of its {@link #serialize() serialized form} is a common overlooked error, this method catches this error
	 * early by not allowing the insertion of data containers.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @param value
	 *            the value to set, or <code>null</code> to remove any currently stored value
	 * @throws UnsupportedOperationException
	 *             if this data container is unmodifiable
	 */
	public void set(String key, Object value);

	/**
	 * Inserts the entries of the given {@link Map} into this data container.
	 * <p>
	 * This converts the keys to Strings and then performs {@link #set(String, Object)} for each entry in the given map.
	 * This will replace any other values that may currently be stored for the same keys.
	 * 
	 * @param values
	 *            the map of values to insert, not <code>null</code>
	 * @throws UnsupportedOperationException
	 *             if this data container is unmodifiable
	 */
	public void setAll(Map<?, ?> values);

	/**
	 * Removes the value for the given key.
	 * 
	 * @param key
	 *            the key, not <code>null</code> or empty
	 * @throws UnsupportedOperationException
	 *             if this data container is unmodifiable
	 */
	public void remove(String key);

	/**
	 * Removes all values from this data container.
	 * 
	 * @throws UnsupportedOperationException
	 *             if this data container is unmodifiable
	 */
	public void clear();

	/**
	 * Gets the number of values that are stored in this data container.
	 * 
	 * @return the number of values
	 */
	public int size();

	/**
	 * Checks if this data container is empty.
	 * 
	 * @return <code>true</code> of this data container is empty
	 */
	public default boolean isEmpty() {
		return (this.size() == 0);
	}

	/**
	 * Gets a {@link Set} of the keys of this data container.
	 * <p>
	 * It is undefined whether or not the returned Set is a modifiable snapshot or an unmodifiable dynamic view on the
	 * current keys.
	 * 
	 * @return a Set of the keys of this data container
	 */
	public Set<String> getKeys();

	/**
	 * Gets a {@link Map} of the contents of this data container.
	 * <p>
	 * It is undefined whether or not the returned Map is a modifiable snapshot or an unmodifiable dynamic view on the
	 * current contents.
	 * 
	 * @return the contents of this data container
	 */
	public Map<String, ?> getValues();

	/**
	 * Gets a (shallow) copy of the contents of this data container as a {@link Map}.
	 * <p>
	 * Unlike {@link #getValues()}, this method is guaranteed to return a new mutable {@link Map} instance instead of an
	 * unmodifiable view.
	 * 
	 * @return the contents of this data container
	 */
	public Map<String, Object> getValuesCopy();

	/**
	 * Gets an unmodifiable view on this data container.
	 * 
	 * @return an unmodifiable data container view
	 */
	public DataContainer asView();

	/**
	 * Gets a serializable representation of this data container.
	 * <p>
	 * Unlike the data container itself, the returned object is suited for serialization. The returned object can be
	 * turned into a data container again via {@link #of(Object)}.
	 * <p>
	 * The returned serializable representation is not a snapshot of the current contents of this data container, but a
	 * dynamic view: Further modifications of this data container write through to the returned serializable
	 * representation.
	 * 
	 * @return the serializable representation, not <code>null</code>
	 */
	public Object serialize();
}
