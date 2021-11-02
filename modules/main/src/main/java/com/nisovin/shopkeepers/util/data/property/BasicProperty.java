package com.nisovin.shopkeepers.util.data.property;

import com.nisovin.shopkeepers.util.data.DataAccessor;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.MissingDataException;
import com.nisovin.shopkeepers.util.data.property.validation.ChainedPropertyValidator;
import com.nisovin.shopkeepers.util.data.property.validation.PropertyValidator;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A basic {@link Property} implementation.
 * <p>
 * After construction, the property needs to be configured and then {@link #build() built} before it is ready to be
 * used. During the configuration phase, one has to at least specify a {@link #name(String) name} and a
 * {@link #dataAccessor(DataAccessor) DataAccessor}. The involved sub classes may require additional configuration steps
 * and impose additional constraints before the property can be built. Also, if the property's value is not configured
 * to be {@link #nullable() nullable} and no non-<code>null</code> {@link #defaultValue(Object) default value} is
 * specified, the resulting property will have no {@link #hasDefaultValue() valid default value}, which may make it
 * difficult to work with in some situations.
 *
 * @param <T>
 *            the type of the value represented by this property
 */
public class BasicProperty<T> implements Property<T> {

	private String name = null;
	private boolean nullable = false;
	// Can be null even if this property does not consider null a valid value.
	private T defaultValue = null;
	private boolean omitIfDefault = false;
	private boolean useDefaultIfMissing = false;
	private PropertyValidator<? super T> validator = null;
	private StringConverter<? super T> stringConverter = StringConverter.DEFAULT;
	private DataAccessor<T> dataAccessor;
	private final DataAccessor<T> unvalidated = new DataAccessor<T>() {
		@Override
		public void save(DataContainer dataContainer, T value) {
			BasicProperty.this.saveUnvalidated(dataContainer, value);
		}

		@Override
		public T load(DataContainer dataContainer) throws InvalidDataException {
			return BasicProperty.this.loadUnvalidated(dataContainer);
		}
	};

	private boolean built = false;

	/**
	 * Creates a new {@link BasicProperty}.
	 */
	public BasicProperty() {
	}

	/**
	 * Checks if this property has already been {@link #build() built}.
	 * 
	 * @return <code>true</code> if this property has already been built
	 */
	protected final boolean isBuilt() {
		return built;
	}

	/**
	 * Verifies that this property has not yet been {@link #build() built}.
	 */
	protected final void validateNotBuilt() {
		Validate.State.isTrue(!this.isBuilt(), "Property has already been built!");
	}

	/**
	 * Builds this property.
	 * <p>
	 * The property can only be built once. This performs remaining setup and validation of this property. Once built,
	 * certain aspects of this property can no longer be further configured.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P build() {
		this.validateNotBuilt();

		// Post construction validation and setup:
		Validate.State.notEmpty(name, "Property name is null or empty!");
		Validate.State.notNull(dataAccessor, "No data accessor has been set!");
		Validate.State.isTrue(!omitIfDefault || this.hasDefaultValue(), "omitIfDefault is set, but property has no default value!");
		Validate.State.isTrue(!useDefaultIfMissing || this.hasDefaultValue(), "useDefaultIfMissing is set, but property has no default value!");

		this.postConstruct();
		if (this.hasDefaultValue()) {
			try {
				this.validateValue(this.getDefaultValue());
			} catch (Exception e) {
				Validate.State.error("The default value for property '" + this.getName()
						+ "' is invalid: " + e.getMessage());
			}
		}
		if (this.isNullable()) {
			try {
				this.validateValue(null);
			} catch (Exception e) {
				Validate.State.error("Null is considered an invalid value, even though property '"
						+ this.getName() + "' is nullable: " + e.getMessage());
			}
		}

		this.built = true;
		return (P) this;
	}

	/**
	 * This is called when the property is {@link #build() built} and can be overridden by sub classes to perform any
	 * remaining setup and validation.
	 */
	protected void postConstruct() {
	}

	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Sets the property name.
	 * <p>
	 * The name has to be set exactly once before the property is {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param name
	 *            the name, not <code>null</code> or empty
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P name(String name) {
		this.validateNotBuilt();
		Validate.State.isTrue(this.name == null, "Another name has already been set!");
		Validate.notEmpty(name, "name is null or empty");
		this.name = name;
		return (P) this;
	}

	@Override
	public final boolean isNullable() {
		return nullable;
	}

	/**
	 * Sets whether the value of this property can be <code>null</code>.
	 * <p>
	 * This changes the behavior of {@link #load(DataContainer)} to return <code>null</code> instead of throwing a
	 * {@link MissingDataException} if there is no data to load the value from.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P nullable() {
		this.validateNotBuilt();
		this.nullable = true;
		return (P) this;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The property may have no valid default value if it is not {@link #isNullable() nullable} and no
	 * non-<code>null</code> default value has been specified during its setup.
	 */
	@Override
	public final boolean hasDefaultValue() {
		return (defaultValue != null) || this.isNullable();
	}

	@Override
	public final T getDefaultValue() {
		Validate.State.isTrue(this.hasDefaultValue(), "This property does not have a valid default value!");
		return defaultValue;
	}

	/**
	 * Sets the default value of this property.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param defaultValue
	 *            the default value, can be <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P defaultValue(T defaultValue) {
		this.validateNotBuilt();
		this.defaultValue = defaultValue;
		return (P) this;
	}

	/**
	 * Sets whether the value of this property should only be saved if it does not match the
	 * {@link #defaultValue(Object) default value}.
	 * <p>
	 * This also automatically activates {@link #useDefaultIfMissing()}.
	 * <p>
	 * When using this option, it is required to specify a {@link #defaultValue(Object) default value}.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P omitIfDefault() {
		this.validateNotBuilt();
		this.omitIfDefault = true;
		this.useDefaultIfMissing();
		return (P) this;
	}

	/**
	 * Sets whether {@link #load(DataContainer)} should automatically return the default value if the data for this
	 * property is missing.
	 * <p>
	 * I.e. with this set, {@link #load(DataContainer)} behaves like {@link #loadOrDefault(DataContainer)}.
	 * <p>
	 * When using this option, it is required to specify a {@link #defaultValue(Object) default value}.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P useDefaultIfMissing() {
		this.validateNotBuilt();
		this.useDefaultIfMissing = true;
		return (P) this;
	}

	/**
	 * Registers the given {@link PropertyValidator}.
	 * <p>
	 * The given validator is invoked by {@link #validateValue(Object)} for non-<code>null</code> values after the
	 * property's internal validations have been passed and can be used to apply additional validations.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * <p>
	 * It is possible to register multiple validators: If multiple validators are registered, they are invoked in the
	 * order of their registration.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param validator
	 *            the validator, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P validator(PropertyValidator<? super T> validator) {
		this.validateNotBuilt();
		Validate.notNull(validator, "validator is null");
		if (this.validator != null) {
			this.validator = new ChainedPropertyValidator<>(this.validator, validator);
		} else {
			this.validator = validator;
		}
		return (P) this;
	}

	@Override
	public final void validateValue(T value) {
		if (value == null) {
			Validate.isTrue(this.isNullable(), "value is null for non-nullable property");
		} else {
			this.internalValidateValue(value);
			if (validator != null) {
				validator.validate(this, value);
			}
		}
	}

	/**
	 * Validates the given value.
	 * <p>
	 * This is called by {@link #validateValue(Object)} before the registered {@link #validator(PropertyValidator)
	 * external validators} are invoked and can be overridden by sub classes to apply additional validations.
	 * <p>
	 * This is not invoked for <code>null</code> values.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is invalid
	 */
	protected void internalValidateValue(T value) {
		// Can be overridden in sub classes.
	}

	/**
	 * Sets the {@link StringConverter} that is used by {@link #toString(Object)}.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param stringConverter
	 *            the String converter, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P stringConverter(StringConverter<? super T> stringConverter) {
		this.validateNotBuilt();
		Validate.notNull(stringConverter, "stringConverter is null");
		this.stringConverter = stringConverter;
		return (P) this;
	}

	@Override
	public final String toString(T value) {
		return stringConverter.toString(value);
	}

	@Override
	public final DataAccessor<T> getDataAccessor() {
		return dataAccessor;
	}

	@Override
	public final DataAccessor<T> unvalidated() {
		return unvalidated;
	}

	/**
	 * Sets the {@link DataAccessor} that is used to save and load values for this property.
	 * <p>
	 * The data accessor has to be set exactly once before the property is {@link #build() built}.
	 * <p>
	 * As a typically useful convenience side effect, if the given data accessor is a {@link DataKeyAccessor} and no
	 * {@link #name(String) property name} has been set yet, this also sets the name of this property to the
	 * {@link DataKeyAccessor#getDataKey() data key} of the given {@link DataKeyAccessor}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param dataAccessor
	 *            the data accessor, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P dataAccessor(DataAccessor<T> dataAccessor) {
		this.validateNotBuilt();
		Validate.notNull(dataAccessor, "dataAccessor is null");
		Validate.State.isTrue(this.dataAccessor == null, "Another DataAccessor has already been set!");
		this.dataAccessor = dataAccessor;
		if (this.getName() == null && dataAccessor instanceof DataKeyAccessor) {
			this.name(((DataKeyAccessor<?>) dataAccessor).getDataKey());
		}
		return (P) this;
	}

	/**
	 * Sets the {@link #dataAccessor(DataAccessor) DataAccessor} to a {@link DataKeyAccessor} that uses the given
	 * {@link DataSerializer} to load and save the property value from the data at the specified data key.
	 * <p>
	 * At the same time, as a typically useful convenience side effect, this method also sets the {@link #name(String)
	 * name} of the property to the given data key, unless a name has already been set.
	 * <p>
	 * The data accessor and property name have to be set exactly once before the property is {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param dataKey
	 *            the data key, not <code>null</code> or empty
	 * @param serializer
	 *            the data serializer, not <code>null</code>
	 * @return this property
	 */
	public final <P extends BasicProperty<T>> P dataKeyAccessor(String dataKey, DataSerializer<T> serializer) {
		// Note: This also automatically set the property name if no name has been set yet.
		return this.dataAccessor(new DataKeyAccessor<>(dataKey, serializer));
	}

	@Override
	public final void save(DataContainer dataContainer, T value) {
		this.validateValue(value);
		this.saveUnvalidated(dataContainer, value);
	}

	private void saveUnvalidated(DataContainer dataContainer, T value) {
		Validate.notNull(dataContainer, "dataContainer is null");
		try {
			if (value == null) {
				this.saveValue(dataContainer, null);
				return;
			}

			// Note: omitIfDefault implies that there is a valid default value.
			if (omitIfDefault && value.equals(this.getDefaultValue())) {
				this.saveValue(dataContainer, null);
				return;
			}

			this.saveValue(dataContainer, value);
		} catch (Exception e) {
			throw new RuntimeException("Failed to save property '" + this.getName() + "': " + e.getMessage(), e);
		}
	}

	/**
	 * Saves the given value to the given {@link DataContainer}.
	 * <p>
	 * This is called by {@link #save(DataContainer, Object)} after the common validation and property logic has been
	 * performed.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 */
	private void saveValue(DataContainer dataContainer, T value) {
		dataAccessor.save(dataContainer, value);
	}

	@Override
	public final T load(DataContainer dataContainer) throws MissingDataException, InvalidDataException {
		T value = this.loadUnvalidated(dataContainer);
		try {
			this.validateValue(value);
		} catch (Exception e) {
			throw new InvalidDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		}
		return value;
	}

	private T loadUnvalidated(DataContainer dataContainer) throws MissingDataException, InvalidDataException {
		Validate.notNull(dataContainer, "dataContainer is null");
		try {
			try {
				return this.loadValue(dataContainer);
			} catch (MissingDataException e) {
				if (useDefaultIfMissing) {
					// This implies that there is a valid default value.
					return this.getDefaultValue();
				} else if (this.isNullable()) {
					return null;
				} else {
					throw e;
				}
			}
		} catch (MissingDataException e) {
			// Separate case to preserve the exception type.
			// Other exception types are not preserved, but callers can figure out the original exception type via the
			// exception cause.
			throw new MissingDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		} catch (InvalidDataException e) {
			throw new InvalidDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		}
	}

	private String loadingFailedErrorMessage(String cause) {
		return "Failed to load property '" + this.getName() + "': " + cause;
	}

	/**
	 * Loads the value for this property from the given {@link DataContainer}.
	 * <p>
	 * This is called by {@link #load(DataContainer)} after the given data container has been validated. This method
	 * does not {@link #validateValue(Object) validate} the loaded value.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @return the loaded value, not <code>null</code>
	 * @throws MissingDataException
	 *             if the data for the value is missing
	 * @throws InvalidDataException
	 *             if the value cannot be loaded
	 */
	private T loadValue(DataContainer dataContainer) throws InvalidDataException {
		return dataAccessor.load(dataContainer);
	}

	@Override
	public final T loadOrDefault(DataContainer dataContainer) throws InvalidDataException {
		try {
			T value = this.load(dataContainer);
			if (value == null) {
				// Data is missing, but the property is nullable.
				// The property being nullable implies that this property has a valid default value.
				assert this.isNullable() && this.hasDefaultValue();
				return this.getDefaultValue(); // Can be null
			} else {
				return value;
			}
		} catch (MissingDataException e) {
			if (this.hasDefaultValue()) {
				return this.getDefaultValue();
			} else {
				throw e;
			}
		}
	}
}
