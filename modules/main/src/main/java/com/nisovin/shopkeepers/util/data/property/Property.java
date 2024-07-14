package com.nisovin.shopkeepers.util.data.property;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataAccessor;
import com.nisovin.shopkeepers.util.data.serialization.DataLoader;
import com.nisovin.shopkeepers.util.data.serialization.DataSaver;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;

/**
 * Represents a particular kind of value.
 * <p>
 * Provides operations to save and load the value to and from a given {@link DataContainer}.
 * <p>
 * While {@link DataSerializer} provides low-level means to serialize and deserialize data types, a
 * {@link Property} may handle additional higher level aspects, such as its association with one or
 * more specific data keys, the definition of default values, and the validation of values specific
 * to the property's usage context and semantic.
 *
 * @param <T>
 *            the type of the represented value
 */
public interface Property<T> extends DataAccessor<T> {

	/**
	 * Gets the name of this property.
	 * <p>
	 * The name should be suitable for users to identify the property in error, log, or other
	 * feedback messages. Properties that store their data under a single data key may for example
	 * use this data key as their name.
	 * 
	 * @return the property name, not <code>null</code> or empty
	 */
	public String getName();

	/**
	 * Gets whether the value of this property can be <code>null</code>.
	 * 
	 * @return <code>true</code> if the value of this property can be <code>null</code>
	 */
	public boolean isNullable();

	/**
	 * Checks if this property has a valid {@link #getDefaultValue() default value}.
	 * 
	 * @return <code>true</code> if this property has a valid default value
	 */
	public boolean hasDefaultValue();

	/**
	 * Gets the default value.
	 * <p>
	 * Make sure to check {@link #hasDefaultValue()} before trying to access the default value.
	 * <p>
	 * The default values returned by subsequent invocations should be {@link Object#equals(Object)
	 * equal}. However, they do not necessarily share the same object identity.
	 * 
	 * @return the default value, can be <code>null</code> if this property is {@link #isNullable()
	 *         nullable}
	 * @throws IllegalStateException
	 *             if this property does not have a {@link #hasDefaultValue() valid default value}
	 */
	public T getDefaultValue();

	/**
	 * Validates the given value.
	 * <p>
	 * This validation is for example performed when the property loads or saves a value.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is invalid
	 */
	public void validateValue(@Nullable T value);

	/**
	 * Converts the given value to a String representation.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @return the String representation, not <code>null</code>
	 */
	public String toString(@Nullable T value);

	/**
	 * Gets the {@link DataLoader} that is used to load values for this property.
	 * <p>
	 * The returned data loader bypasses all value validations and other logic that is provided by
	 * this property on top of the data loader's own logic.
	 * 
	 * @return the data loader, not <code>null</code>
	 */
	public DataLoader<? extends T> getLoader();

	/**
	 * Gets the {@link DataSaver} that is used to save values for this property.
	 * <p>
	 * The returned data saver bypasses the value validations and other logic that is provided by
	 * this property on top of the data saver's own logic.
	 * 
	 * @return the data saver, not <code>null</code>
	 */
	public DataSaver<? super T> getSaver();

	/**
	 * Gets a {@link DataLoader} that loads values for this property just like the property itself,
	 * except that the loaded values are not {@link #validateValue(Object) validated}.
	 * 
	 * @return the unvalidated property data loader, not <code>null</code>
	 */
	public DataLoader<? extends T> getUnvalidatedLoader();

	/**
	 * Gets a {@link DataAccessor} that saves and loads values for this property just like the
	 * property itself, except that the saved and loaded values are not
	 * {@link #validateValue(Object) validated}.
	 * 
	 * @return the unvalidated property data accessor, not <code>null</code>
	 */
	public DataSaver<? super T> getUnvalidatedSaver();

	/**
	 * Saves the given property value to the given {@link DataContainer}.
	 * <p>
	 * This {@link #validateValue(Object) validates} the given value prior to storing its serialized
	 * data in the given data container.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 */
	@Override
	public void save(DataContainer dataContainer, @Nullable T value);

	/**
	 * Loads the value for this property from the given {@link DataContainer}.
	 * <p>
	 * This assumes that any potentially necessary data migrations have already been applied to the
	 * given data.
	 * <p>
	 * If the data is missing for this property, the property may either return a fallback value
	 * (which might be <code>null</code>), or throw a {@link MissingDataException}. The exact
	 * behavior depends on the concrete property implementation. To always take the
	 * {@link #getDefaultValue() default value} into account, one can use
	 * {@link #loadOrDefault(DataContainer)}.
	 * 
	 * @param dataContainer
	 *            the data container not <code>null</code>
	 * @return the loaded value, or an optional fallback value (which can be <code>null</code>) if
	 *         there is no data for this property
	 * @throws MissingDataException
	 *             if the data for the value is missing and this property does not provide a
	 *             fallback value
	 * @throws InvalidDataException
	 *             if the value cannot be loaded or is {@link #validateValue(Object) invalid}
	 */
	@Override
	public T load(DataContainer dataContainer) throws InvalidDataException;

	/**
	 * Loads the value for this property from the given {@link DataContainer}.
	 * <p>
	 * This loads the value like {@link #load(DataContainer)}, but always returns the property's
	 * {@link #getDefaultValue() default value} if the data for the property is missing. If the data
	 * is missing and this property has no {@link #hasDefaultValue() valid default value}, this
	 * throws a {@link MissingDataException} just like {@link #load(DataContainer)}.
	 * 
	 * @param dataContainer
	 *            the data container not <code>null</code>
	 * @return the loaded value, can be <code>null</code> if there is no data for this property and
	 *         the default value is <code>null</code>
	 * @throws MissingDataException
	 *             if the data for the value is missing and this property has no valid default value
	 * @throws InvalidDataException
	 *             if the value cannot be loaded or is {@link #validateValue(Object) invalid}
	 * @see #load(DataContainer)
	 */
	public T loadOrDefault(DataContainer dataContainer) throws InvalidDataException;
}
