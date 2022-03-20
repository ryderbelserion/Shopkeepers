package com.nisovin.shopkeepers.util.data.container.value;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * A container for a single non-<code>null</code> value of data.
 * <p>
 * An implementation may either store the value itself, or it may provide a view on the value that
 * is stored in an underlying data structure, such as in an entry of a {@link DataContainer}.
 * <p>
 * The value may or may not be {@link #isPresent() present}.
 * <p>
 * Some implementations may not allow {@link #set(Object) setting} the value.
 * <p>
 * In order to serve as an intermediate representation of data, independent of any concrete storage
 * format, the same limitations apply to the stored value as for values stored by
 * {@link DataContainer}s. Directly serializing {@link DataValue}s themselves might not be
 * supported.
 * <p>
 * {@link Object#equals(Object)} compares {@link DataValue}s based on their stored value.
 * 
 * @see DataContainer
 */
public interface DataValue {

	/**
	 * Creates a new empty {@link DataValue} that is not bound to any underlying data structure.
	 * 
	 * @return the newly created {@link DataValue}
	 */
	public static DataValue create() {
		return of(null);
	}

	/**
	 * Creates a new {@link DataValue} that is not bound to any underlying data structure.
	 * 
	 * @param value
	 *            the initially stored value, or <code>null</code>
	 * @return the newly created {@link DataValue}
	 */
	public static DataValue of(@Nullable Object value) {
		return new SimpleDataValue(value);
	}

	/////

	/**
	 * Checks if the value is present.
	 * 
	 * @return <code>true</code> if the value is present
	 */
	public default boolean isPresent() {
		return (this.get() != null);
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value, or <code>null</code> if the value is not present
	 */
	public default @Nullable Object get() {
		return this.getOrDefault(null);
	}

	/**
	 * Gets the value.
	 * <p>
	 * If the value is not present, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the value, or the given default value if the value is not present
	 */
	public @Nullable Object getOrDefault(@Nullable Object defaultValue);

	/**
	 * Checks if the value is present and of the specified type.
	 * <p>
	 * This does not check if the value can be converted to the specified type.
	 * 
	 * @param type
	 *            the expected type of the value
	 * @return <code>true</code> if the value is present and of the specified type
	 */
	public boolean isOfType(Class<?> type);

	/**
	 * Gets the value of the specified type.
	 * 
	 * @param <T>
	 *            the expected type of the value
	 * @param type
	 *            the expected type of the value
	 * @return the value, or <code>null</code> if the value is not of the specified type
	 */
	public default <T> @Nullable T getOfType(Class<T> type) {
		return this.getOfTypeOrDefault(type, null);
	}

	/**
	 * Gets the value of the specified type.
	 * <p>
	 * If the value is not of the specified type, this returns the specified default value.
	 * 
	 * @param <T>
	 *            the expected type of the value
	 * @param type
	 *            the expected type of the value
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the value, or the given default value if the value is not of the specified type
	 */
	public <T> @Nullable T getOfTypeOrDefault(Class<T> type, @Nullable T defaultValue);

	/**
	 * Checks if the value is present and a {@link String}.
	 * <p>
	 * This does not check if the value can be converted to a String.
	 * 
	 * @return <code>true</code> if the value is present and a String
	 */
	public default boolean isString() {
		Object value = this.get();
		return (value instanceof String);
	}

	/**
	 * Gets the value converted to a {@link String}.
	 * 
	 * @return the String, or <code>null</code> if the value cannot be converted to a String
	 */
	public default @Nullable String getString() {
		return this.getStringOrDefault(null);
	}

	/**
	 * Gets the value converted to a {@link String}.
	 * <p>
	 * If the value cannot be converted to a String, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the String, or the given default value if the value cannot be converted to a String
	 */
	public @Nullable String getStringOrDefault(@Nullable String defaultValue);

	/**
	 * Checks if the value is present and a {@link Number}.
	 * <p>
	 * This does not check if the value can be converted to a Number.
	 * 
	 * @return <code>true</code> if the value is present and a Number
	 */
	public default boolean isNumber() {
		Object value = this.get();
		return (value instanceof Number);
	}

	/**
	 * Gets the value converted to an {@link Integer}.
	 * 
	 * @return the Integer (not <code>null</code>), or <code>0</code> if the value cannot be
	 *         converted to an Integer
	 */
	public default int getInt() {
		return this.getIntOrDefault(0);
	}

	/**
	 * Gets the value converted to an {@link Integer}.
	 * <p>
	 * If the value cannot be converted to an Integer, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value
	 * @return the Integer (not <code>null</code>), or the given default value if the value cannot
	 *         be converted to an Integer
	 */
	public int getIntOrDefault(int defaultValue);

	/**
	 * Gets the value converted to a {@link Long}.
	 * 
	 * @return the Long (not <code>null</code>), or <code>0L</code> if the value cannot be converted
	 *         to a Long
	 */
	public default long getLong() {
		return this.getLongOrDefault(0L);
	}

	/**
	 * Gets the value converted to a {@link Long}.
	 * <p>
	 * If the value cannot be converted to a Long, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value
	 * @return the Long (not <code>null</code>), or the given default value if the value cannot be
	 *         converted to a Long
	 */
	public long getLongOrDefault(long defaultValue);

	/**
	 * Gets the value converted to a {@link Float}.
	 * 
	 * @return the Float (not <code>null</code>), or <code>0.0F</code> if the value cannot be
	 *         converted to a Float
	 */
	public default float getFloat() {
		return this.getFloatOrDefault(0.0F);
	}

	/**
	 * Gets the value converted to a {@link Float}.
	 * <p>
	 * If the value cannot be converted to a Float, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value
	 * @return the Float (not <code>null</code>), or the given default value if the value cannot be
	 *         converted to a Float
	 */
	public float getFloatOrDefault(float defaultValue);

	/**
	 * Gets the value converted to a {@link Double}.
	 * 
	 * @return the Double (not <code>null</code>), or <code>0.0D</code> if the value cannot be
	 *         converted to a Double
	 */
	public default double getDouble() {
		return this.getDoubleOrDefault(0.0D);
	}

	/**
	 * Gets the value converted to a {@link Double}.
	 * <p>
	 * If the value cannot be converted to a Double, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value
	 * @return the Double (not <code>null</code>), or the given default value if the value cannot be
	 *         converted to a Double
	 */
	public double getDoubleOrDefault(double defaultValue);

	/**
	 * Checks if the value is present and a {@link Boolean}.
	 * <p>
	 * This does not check if the value can be converted to a Boolean.
	 * 
	 * @return <code>true</code> if the value is present and a Boolean
	 */
	public default boolean isBoolean() {
		Object value = this.get();
		return (value instanceof Boolean);
	}

	/**
	 * Gets the value converted to a {@link Boolean}.
	 * 
	 * @return the Boolean (not <code>null</code>), or <code>false</code> if the value cannot be
	 *         converted to a Boolean
	 */
	public default boolean getBoolean() {
		return this.getBooleanOrDefault(false);
	}

	/**
	 * Gets the value converted to a {@link Boolean}.
	 * <p>
	 * If the value cannot be converted to a Boolean, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value
	 * @return the Boolean (not <code>null</code>), or the given default value if the value cannot
	 *         be converted to a Boolean
	 */
	public boolean getBooleanOrDefault(boolean defaultValue);

	/**
	 * Checks if the value is present and a {@link List}.
	 * 
	 * @return <code>true</code> if the value is present and a List
	 */
	public default boolean isList() {
		Object value = this.get();
		return (value instanceof List);
	}

	/**
	 * Gets the value as a {@link List}.
	 * 
	 * @return the List, or <code>null</code> if the value is not a List
	 */
	public default @Nullable List<?> getList() {
		return this.getListOrDefault(null);
	}

	/**
	 * Gets the value as a {@link List}.
	 * <p>
	 * If the value is not a List, this returns the specified default value.
	 * 
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return the List, or the given default value if the value is not a List
	 */
	public default @Nullable List<?> getListOrDefault(@Nullable List<?> defaultValue) {
		Object value = this.get();
		if (value instanceof List) {
			return (List<?>) value;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks if the value is present and can be {@link DataContainer#of(Object) converted} to a
	 * {@link DataContainer}.
	 * 
	 * @return <code>true</code> if the value is present and can be converted to a data container
	 */
	public default boolean isContainer() {
		Object value = this.get();
		return DataContainer.isDataContainer(value);
	}

	/**
	 * Gets the value {@link DataContainer#of(Object) converted} to a {@link DataContainer}.
	 * 
	 * @return the data container, or <code>null</code> if the value cannot be converted to a data
	 *         container
	 */
	public default @Nullable DataContainer getContainer() {
		Object value = this.get();
		return DataContainer.of(value);
	}

	/**
	 * Creates a new empty {@link DataContainer} and stores its {@link DataContainer#serialize()
	 * serialized form} as value.
	 * <p>
	 * The returned data container reads and writes through to its stored serialized form.
	 * <p>
	 * This will replace any other value that may currently be stored.
	 * 
	 * @return the newly created data container
	 * @throws UnsupportedOperationException
	 *             if this {@link DataValue} is unmodifiable
	 */
	public default DataContainer createContainer() {
		DataContainer container = DataContainer.create();
		this.set(container.serialize());
		return container;
	}

	/**
	 * Sets the value.
	 * <p>
	 * This will replace any other value that may currently be stored.
	 * <p>
	 * If the given value is <code>null</code>, this will clear the currently stored value by
	 * delegating the method call to {@link #clear()}.
	 * <p>
	 * Because {@link DataContainer}s and {@link DataValue}s themselves are not guaranteed to be
	 * serializable and storing them instead of their {@link DataContainer#serialize() serialized
	 * form} or underlying {@link #get() value} is a commonly overlooked error, this method catches
	 * this error early by not allowing to store a {@link DataContainer} or {@link DataValue}.
	 * 
	 * @param value
	 *            the value to set, or <code>null</code> to remove any currently stored value
	 * @throws UnsupportedOperationException
	 *             if this {@link DataValue} is unmodifiable
	 */
	public void set(@Nullable Object value);

	/**
	 * Clears the value.
	 * 
	 * @throws UnsupportedOperationException
	 *             if this {@link DataValue} is unmodifiable
	 */
	public void clear();

	/**
	 * Gets an unmodifiable view on this {@link DataValue}.
	 * 
	 * @return an unmodifiable {@link DataValue} view
	 */
	public DataValue asView();
}
