package com.nisovin.shopkeepers.property;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Stores a value associated with a specific {@link PropertyContainer}.
 * <p>
 * Provides operations to get, set, save, and load the value.
 * <p>
 * After construction, the property needs to be configured and then {@link #build(PropertyContainer) built} before it is
 * ready to be used. During the configuration phase it is required to specify a {@link #key(String) storage key}. Also,
 * unless the property's value is configured to be {@link #nullable() nullable}, a non-<code>null</code>
 * {@link #defaultValue(Object) default value} has to be specified. The involved sub classes may require additional
 * configuration steps and impose additional constraints before the property can be built.
 *
 * @param <T>
 *            the type of the stored value
 */
public abstract class Property<T> {

	/**
	 * A flag that specifies an additional aspect of how {@link Property#setValue(Object, Set)} behaves.
	 * <p>
	 * {@link UpdateFlag#toString()} should return a String that allows the identification of this update flag.
	 */
	public interface UpdateFlag {
	}

	/**
	 * The default {@link UpdateFlag}s.
	 */
	public static enum DefaultUpdateFlag implements UpdateFlag {
		/**
		 * Whether to mark the {@link Property#getContainer() container} as {@link AbstractPropertyContainer#markDirty()
		 * dirty} when the value changed.
		 */
		MARK_DIRTY
	}

	/**
	 * An unmodifiable set of the {@link UpdateFlag}s that are used by {@link #setValue(Object)}, i.e. when no update
	 * flags were specified.
	 */
	public static final Set<UpdateFlag> DEFAULT_UPDATE_FLAGS = Collections.singleton(DefaultUpdateFlag.MARK_DIRTY);

	private AbstractPropertyContainer container; // Not null once built
	private String key; // Not null or empty
	private boolean nullable = false;
	private T defaultValue = null; // Not null if Property is not nullable
	private PropertyMigrator migrator = null;
	private PropertyValidator<T> validator = null;
	private ValueChangeListener<T> valueChangeListener = null;

	private T value;

	/**
	 * Creates a new {@link Property}.
	 * <p>
	 * See the description of {@link Property} for the additionally required configuration steps before the property is
	 * ready to be used.
	 */
	public Property() {
	}

	/**
	 * Checks if this property has already been {@link #build(PropertyContainer) built} and added to a
	 * {@link #getContainer() container}.
	 * 
	 * @return <code>true</code> if this property has already been built
	 */
	private final boolean isBuilt() {
		return (container != null);
	}

	/**
	 * Verifies that this property has already been {@link #build() built}.
	 */
	protected final void validateBuilt() {
		Validate.State.isTrue(this.isBuilt(), "Property has not yet been built!");
	}

	/**
	 * Verifies that this property has not yet been {@link #build() built}.
	 */
	protected final void validateNotBuilt() {
		Validate.State.isTrue(!this.isBuilt(), "Property has already been built!");
	}

	/**
	 * Builds this property and adds it to the given {@link PropertyContainer}.
	 * <p>
	 * The property can only be built once. Building may perform remaining setup and validation of this property. Once
	 * built, certain aspects of this property can no longer be further configured.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param container
	 *            the container, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P build(PropertyContainer container) {
		this.validateNotBuilt();
		Validate.notNull(container, "container is null");
		Validate.isTrue(container instanceof PropertyContainer, "container is not of type AbstractPropertyContainer");

		// Post construction validation and setup:
		Validate.State.notNull(key, "No key has been set!");
		this.postConstruct();
		try {
			this.validateValue(defaultValue);
		} catch (Exception e) {
			Validate.State.error("Default value is invalid: " + e.getMessage());
		}
		value = defaultValue;

		this.container = (AbstractPropertyContainer) container;
		this.container.add(this);
		return (P) this;
	}

	/**
	 * This is called when the property is {@link #build() built} and can be overridden by sub classes to perform any
	 * remaining setup and validation.
	 */
	protected void postConstruct() {
	}

	/**
	 * Gets the {@link PropertyContainer} this property has been added to.
	 * 
	 * @return the container, not <code>null</code>
	 */
	public final PropertyContainer getContainer() {
		return container;
	}

	/**
	 * Gets the storage key.
	 * 
	 * @return the storage key
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Sets the storage key.
	 * <p>
	 * This method needs to be called exactly once before the property is {@link #build(PropertyContainer) built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param key
	 *            the storage key, not <code>null</code> or empty, has to be unique among all properties added to the
	 *            same container
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P key(String key) {
		this.validateNotBuilt();
		Validate.State.isTrue(this.key == null, "A key has already been set!");
		Validate.notEmpty(key, "key is null or empty");
		// TODO Validate that the key is valid for use as data key?
		this.key = key;
		return (P) this;
	}

	/**
	 * Gets whether the value of this property can be <code>null</code>.
	 * 
	 * @return <code>true</code> if the value of this property can be <code>null</code>
	 */
	public final boolean isNullable() {
		return nullable;
	}

	/**
	 * Sets whether the value of this property can be <code>null</code>.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build(PropertyContainer) built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P nullable() {
		this.validateNotBuilt();
		this.nullable = true;
		return (P) this;
	}

	/**
	 * Gets the default value.
	 * 
	 * @return the default value
	 */
	public final T getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Sets the default value of this property.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build(PropertyContainer) built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param defaultValue
	 *            the default value
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P defaultValue(T defaultValue) {
		this.validateNotBuilt();
		this.defaultValue = defaultValue;
		return (P) this;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public final T getValue() {
		return value;
	}

	/**
	 * Registers the given {@link PropertyValidator}.
	 * <p>
	 * This method can only be called once (it is not possible to register multiple validators), and only while the
	 * property has not yet been {@link #build(PropertyContainer) built}.
	 * <p>
	 * The given validator is invoked by {@link #validateValue(Object)} after the property's internal validations have
	 * been passed and can be used to apply additional validations.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param validator
	 *            the validator, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P validator(PropertyValidator<T> validator) {
		this.validateNotBuilt();
		Validate.State.isTrue(this.validator == null, "Another validator has already been set!");
		Validate.notNull(validator, "validator is null");
		this.validator = validator;
		return (P) this;
	}

	/**
	 * Validates the given value.
	 * <p>
	 * This is for example called by {@link #setValue(Object, Set)} whenever a new value is about to be set.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is considered invalid
	 */
	public final void validateValue(T value) {
		Validate.isTrue(value != null || this.isNullable(), "value is null for non-nullable property");
		this.internalValidateValue(value);
		if (validator != null) {
			validator.validate(this, value);
		}
	}

	/**
	 * Validates the given value.
	 * <p>
	 * This is called by {@link #validateValue(Object)} before the registered {@link #validator(PropertyValidator)
	 * external validator} is invoked and can be overridden by sub classes to apply additional validations.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is considered invalid
	 */
	protected void internalValidateValue(T value) {
		// Can be overridden in sub classes.
	}

	/**
	 * Sets the value.
	 * <p>
	 * This calls {@link #setValue(Object, Set)} using {@link #DEFAULT_UPDATE_FLAGS} for the update flags.
	 * 
	 * @param value
	 *            the new value
	 */
	public final void setValue(T value) {
		this.setValue(value, DEFAULT_UPDATE_FLAGS);
	}

	/**
	 * Sets the value.
	 * <p>
	 * The given {@link UpdateFlag}s specify additional aspects of how this operation behaves.
	 * 
	 * @param value
	 *            the new value
	 * @param updateFlags
	 *            the update flags
	 */
	public final void setValue(T value, Set<? extends UpdateFlag> updateFlags) {
		if (updateFlags == null) updateFlags = Collections.emptySet();

		// Transformation and validation:
		T newValue = this.transformValue(value);
		this.validateValue(newValue);

		// Update the value:
		T oldValue = this.getValue();
		if (Objects.equals(oldValue, newValue)) return; // Value has not changed
		this.value = newValue;

		// Post-value-change actions:
		this.onValueChanged(oldValue, newValue, updateFlags);
		if (valueChangeListener != null) {
			valueChangeListener.onValueChanged(this, oldValue, newValue, updateFlags);
		}
		if (updateFlags.contains(DefaultUpdateFlag.MARK_DIRTY)) {
			container.markDirty();
		}
	}

	/**
	 * This method is invoked by {@link #setValue(Object, Set)} and can be used to transform the provided value before
	 * it is {@link #validateValue(Object) validated} and applied to this property.
	 * 
	 * @param value
	 *            the original new value
	 * @return the transformed new value
	 */
	protected T transformValue(T value) {
		// Can be overridden to apply transformations.
		return value;
	}

	/**
	 * This method is invoked whenever the value of this property has changed.
	 * <p>
	 * This method might not be invoked for calls to {@link #setValue(Object, Set)} if the value did not actually
	 * change.
	 * 
	 * @param oldValue
	 *            the old value
	 * @param newValue
	 *            the new value
	 * @param updateFlags
	 *            the update flags, not <code>null</code>
	 */
	protected void onValueChanged(T oldValue, T newValue, Set<? extends UpdateFlag> updateFlags) {
		// Can be overridden to react to value changes.
	}

	/**
	 * Registers the given {@link ValueChangeListener} to react to value changes.
	 * <p>
	 * This method can only be called once (it is not possible to register multiple {@link ValueChangeListener}s), and
	 * only while the property has not yet been {@link #build(PropertyContainer) built}.
	 * <p>
	 * The given listener might not be invoked for calls to {@link Property#setValue(Object, Set)} if the value did not
	 * actually change.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param listener
	 *            the value change listener, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P onValueChanged(ValueChangeListener<T> listener) {
		this.validateNotBuilt();
		Validate.State.isTrue(this.valueChangeListener == null, "Another listener has already been set!");
		Validate.notNull(listener, "listener is null");
		this.valueChangeListener = listener;
		return (P) this;
	}

	/**
	 * Registers a {@link ValueChangeListener} that reacts to value changes by calling the given {@link Runnable}.
	 * <p>
	 * This method is a shortcut for creating and {@link #onValueChanged(ValueChangeListener) registering} a
	 * {@link ValueChangeListener} that calls the given {@link Runnable}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param runnable
	 *            the runnable, not <code>null</code>
	 * @return this property
	 * @see #onValueChanged(ValueChangeListener)
	 */
	public final <P extends Property<T>> P onValueChanged(Runnable runnable) {
		Validate.notNull(runnable, "runnable is null");
		return this.onValueChanged((property, oldValue, newValue, updateFlags) -> runnable.run());
	}

	/**
	 * Converts the given value to a String that can for example be used in exception messages, or to represent the
	 * value to a user.
	 * 
	 * @param value
	 *            the value, can be <code>null</code>
	 * @return the String representation, not <code>null</code>
	 */
	public String toString(T value) {
		return String.valueOf(value);
	}

	/**
	 * Creates an {@link InvalidDataException} that is used when the value for this property is missing.
	 * 
	 * @return the {@link InvalidDataException} for a missing value
	 */
	protected InvalidDataException missingValueError() {
		return new InvalidDataException("Missing value.");
	}

	/**
	 * Registers the given {@link PropertyMigrator}.
	 * <p>
	 * This method can only be called once (it is not possible to register multiple migrators), and only while the
	 * property has not yet been {@link #build(PropertyContainer) built}.
	 * <p>
	 * The given migrator is invoked by {@link #load(DataContainer)} and can be used to apply data migrations to the
	 * involved data container before the property attempts to load the value.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param migrator
	 *            the migrator, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends Property<T>> P migrator(PropertyMigrator migrator) {
		this.validateNotBuilt();
		Validate.State.isTrue(this.migrator == null, "Another migrator has already been set!");
		Validate.notNull(migrator, "migrator is null");
		this.migrator = migrator;
		return (P) this;
	}

	/**
	 * Loads the value of this property from the given {@link DataContainer}.
	 * <p>
	 * This first invokes the registered {@link #migrator(PropertyMigrator) migrator} to apply any necessary data
	 * migrations and then uses {@link #deserializeValue(Object)} to reconstruct the property value from the data stored
	 * at the property's {@link #getKey() key}.
	 * <p>
	 * If the value cannot be loaded for some reason, a warning is logged, the {@link #getDefaultValue() default value}
	 * is used, and the {@link #getContainer() container} is marked as {@link AbstractPropertyContainer#markDirty()
	 * dirty}.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 */
	public final void load(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		if (migrator != null) {
			migrator.migrate(this, dataContainer);
		}

		try {
			T value;
			Object dataObject = dataContainer.get(key);
			if (dataObject == null) {
				if (!this.isNullable()) {
					throw this.missingValueError();
				} else {
					value = null;
				}
			} else {
				// This is expected to throw an exception if it cannot reconstruct the original value:
				value = this.deserializeValue(dataObject);
				Validate.notNull(value, () -> "Property of type " + this.getClass().getName()
						+ " deserialized data <" + dataObject + "> to null!");
			}

			// This may throw an exception if the loaded value is invalid:
			// TODO Allow callers to pass UpdateFlags to this method?
			this.setValue(value, Collections.emptySet()); // Does not mark the container dirty
		} catch (Exception e) {
			Log.warning(container.getLogPrefix() + "Failed to load property '" + key + "': " + e.getMessage()
					+ " Using the default value now: '" + this.toString(defaultValue) + "'");
			// The default value is expected to pass the validation.
			// This marks the container as dirty.
			this.setValue(defaultValue);
		}
	}

	/**
	 * Reconstructs the value for this property from the given data object.
	 * <p>
	 * This method does not ensure that the reconstructed value is valid according to {@link #validateValue(Object)}.
	 * 
	 * @param dataObject
	 *            the data object, not <code>null</code>
	 * @return the reconstructed value, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the value can not be reconstructed
	 */
	protected abstract T deserializeValue(Object dataObject) throws InvalidDataException;

	/**
	 * Saves the value of this property into the given {@link DataContainer}.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 */
	public final void save(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		if (value == null) return; // Nothing to save
		Object serialized = this.serializeValue(value);
		Validate.notNull(serialized, () -> "Property '" + key + "' of type " + this.getClass().getName()
				+ " serialized value <" + value + "> to null!");
		dataContainer.set(key, serialized);
	}

	/**
	 * Converts the given value to a representation that can be serialized as described by {@link DataContainer}.
	 * <p>
	 * The value can be reconstructed from the serialized representation via {@link #deserializeValue(Object)}.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 * @return the serialized representation, not <code>null</code>
	 */
	protected abstract Object serializeValue(T value);
}
