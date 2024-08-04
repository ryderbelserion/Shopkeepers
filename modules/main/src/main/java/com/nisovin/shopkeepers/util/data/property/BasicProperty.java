package com.nisovin.shopkeepers.util.data.property;

import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.validation.ChainedPropertyValidator;
import com.nisovin.shopkeepers.util.data.property.validation.PropertyValidator;
import com.nisovin.shopkeepers.util.data.serialization.DataAccessor;
import com.nisovin.shopkeepers.util.data.serialization.DataLoader;
import com.nisovin.shopkeepers.util.data.serialization.DataSaver;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A basic {@link Property} implementation.
 * <p>
 * After construction, the property needs to be configured and then {@link #build() built} before it
 * is ready to be used. During the configuration phase, one has to at least specify a
 * {@link #name(String) name} and a {@link #dataAccessor(DataLoader) DataAccessor}. The involved
 * subclasses may require additional configuration steps and impose additional constraints before
 * the property can be built. Also, if the property's value is not configured to be
 * {@link #nullable() nullable} and no non-<code>null</code> {@link #defaultValue(Object) default
 * value} is specified, the resulting property will have no {@link #hasDefaultValue() valid default
 * value}, which may make it difficult to work with in some situations.
 *
 * @param <T>
 *            the type of the value represented by this property
 */
public class BasicProperty<T> implements Property<T> {

	private @Nullable String name = null;
	private boolean nullable = false;
	// Can be null even if this property does not consider null a valid value.
	private @Nullable T defaultValue = null;
	private @Nullable Supplier<@Nullable T> defaultValueSupplier = null;
	private boolean omitIfDefault = false;
	private boolean useDefaultIfMissing = false;
	private @Nullable PropertyValidator<? super @NonNull T> validator = null;
	private StringConverter<? super T> stringConverter = StringConverter.DEFAULT;
	private @Nullable DataLoader<? extends T> dataLoader;
	private @Nullable DataSaver<? super T> dataSaver;

	private final DataLoader<T> unvalidatedDataLoader = new DataLoader<T>() {
		@Override
		public T load(DataContainer dataContainer) throws InvalidDataException {
			return Unsafe.initialized(BasicProperty.this).loadUnvalidated(dataContainer);
		}
	};
	private final DataSaver<T> unvalidatedDataSaver = new DataSaver<T>() {
		@Override
		public void save(DataContainer dataContainer, @Nullable T value) {
			Unsafe.initialized(BasicProperty.this).saveUnvalidated(dataContainer, value);
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
	 * Verifies that this property has already been {@link #build() built}.
	 */
	protected final void validateBuilt() {
		Validate.State.isTrue(this.isBuilt(), "Property has not yet been built!");
	}

	/**
	 * Builds this property.
	 * <p>
	 * The property can only be built once. This performs remaining setup and validation of this
	 * property. Once built, certain aspects of this property can no longer be further configured.
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
		Validate.State.notNull(dataLoader, "No data accessor has been set!");
		assert dataSaver != null;
		Validate.State.isTrue(!omitIfDefault || this.hasDefaultValue(),
				"omitIfDefault is set, but property has no default value!");
		Validate.State.isTrue(!useDefaultIfMissing || this.hasDefaultValue(),
				"useDefaultIfMissing is set, but property has no default value!");

		this.postConstruct();
		if (this.hasDefaultValue()) {
			try {
				// If a defaultValueSupplier is used: Generate, validate, and then discard one value
				// to catch errors with the values produced by the supplier early.
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
	 * This is called when the property is {@link #build() built} and can be overridden by
	 * subclasses to perform any remaining setup and validation.
	 */
	protected void postConstruct() {
	}

	@Override
	public final String getName() {
		Validate.State.notNull(name, "name has not yet been set");
		return Unsafe.assertNonNull(name);
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
	 * This changes the behavior of {@link #load(DataContainer)} to return <code>null</code> instead
	 * of throwing a {@link MissingDataException} if there is no data to load the value from.
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
	 * The property may have no valid default value if it is not {@link #isNullable() nullable} and
	 * no non-<code>null</code> default value has been specified during its setup.
	 */
	@Override
	public final boolean hasDefaultValue() {
		return defaultValue != null || defaultValueSupplier != null || this.isNullable();
	}

	@Override
	public final T getDefaultValue() {
		Validate.State.isTrue(this.hasDefaultValue(),
				"This property does not have a valid default value!");
		if (defaultValueSupplier != null) {
			return Unsafe.cast(defaultValueSupplier.get());
		}

		return Unsafe.cast(defaultValue);
	}

	/**
	 * Sets the default value of this property and clears any previously specified
	 * {@link #defaultValueSupplier(Supplier)}.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param defaultValue
	 *            the default value, can be <code>null</code> if this property is
	 *            {@link #isNullable() nullable}
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P defaultValue(T defaultValue) {
		this.validateNotBuilt();
		this.defaultValue = defaultValue;
		this.defaultValueSupplier = null;
		return (P) this;
	}

	/**
	 * Sets the default value supplier of this property and clears any previously specified
	 * {@link #defaultValue(Object)}.
	 * <p>
	 * The supplier is expected to return {@link Object#equals(Object) equal} values on each
	 * invocation, but is allowed to return objects with different object identities. See
	 * {@link #getDefaultValue()}.
	 * <p>
	 * The values returned by the supplier are validated like any other property value:
	 * <code>null</code> is only allowed if this property is {@link #isNullable() nullable}. The
	 * supplier is also invoked during {@link #build()} and the returned value is validated to catch
	 * any errors with the returned values early.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param defaultValueSupplier
	 *            the default value supplier, or <code>null</code> to clear the current supplier
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P defaultValueSupplier(
			@Nullable Supplier<@Nullable T> defaultValueSupplier
	) {
		this.validateNotBuilt();
		this.defaultValueSupplier = defaultValueSupplier;
		this.defaultValue = null;
		return (P) this;
	}

	/**
	 * Sets whether the value of this property should only be saved if it does not match the
	 * {@link #defaultValue(Object) default value}.
	 * <p>
	 * This also automatically activates {@link #useDefaultIfMissing()}.
	 * <p>
	 * When using this option, it is required to specify a {@link #defaultValue(Object) default
	 * value}.
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
	 * Sets whether {@link #load(DataContainer)} should automatically return the default value if
	 * the data for this property is missing.
	 * <p>
	 * I.e. with this set, {@link #load(DataContainer)} behaves like
	 * {@link #loadOrDefault(DataContainer)}.
	 * <p>
	 * When using this option, it is required to specify a {@link #defaultValue(Object) default
	 * value}.
	 * <p>
	 * Be aware that by using this option, components that call {@link #load(DataContainer)} are no
	 * longer able to detect and react to cases of missing property data. Consequently, only use
	 * this option for properties for which missing data is either an expected state (e.g. when
	 * using {@link #omitIfDefault()}), or for which the explicit handling of the missing data case
	 * is not necessary, for example because their default values represent the missing data case
	 * equally well (this may for example apply when using an empty String or an empty List as
	 * default value).
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
	 * The given validator is invoked by {@link #validateValue(Object)} for non-<code>null</code>
	 * values after the property's internal validations have been passed and can be used to apply
	 * additional validations.
	 * <p>
	 * This method can only be called while the property has not yet been {@link #build() built}.
	 * <p>
	 * It is possible to register multiple validators: If multiple validators are registered, they
	 * are invoked in the order of their registration.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param validator
	 *            the validator, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>> P validator(
			PropertyValidator<? super @NonNull T> validator
	) {
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
	public final void validateValue(@Nullable T value) {
		if (value == null) {
			Validate.isTrue(this.isNullable(), "value is null for non-nullable property");
		} else {
			this.internalValidateValue(value);
			if (validator != null) {
				validator.validate(value);
			}
		}
	}

	/**
	 * Validates the given value.
	 * <p>
	 * This is called by {@link #validateValue(Object)} before the registered
	 * {@link #validator(PropertyValidator) external validators} are invoked and can be overridden
	 * by subclasses to apply additional validations.
	 * <p>
	 * This is not invoked for <code>null</code> values.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 * @throws RuntimeException
	 *             if the given value is invalid
	 */
	protected void internalValidateValue(@NonNull T value) {
		// Can be overridden in subclasses.
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
	public final <P extends BasicProperty<T>> P stringConverter(
			StringConverter<? super T> stringConverter
	) {
		this.validateNotBuilt();
		Validate.notNull(stringConverter, "stringConverter is null");
		this.stringConverter = stringConverter;
		return (P) this;
	}

	@Override
	public final String toString(@Nullable T value) {
		return stringConverter.toString(value);
	}

	@Override
	public final DataLoader<? extends T> getLoader() {
		this.validateBuilt();
		return Unsafe.assertNonNull(dataLoader);
	}

	@Override
	public final DataSaver<? super T> getSaver() {
		this.validateBuilt();
		return Unsafe.assertNonNull(dataSaver);
	}

	@Override
	public final DataLoader<? extends T> getUnvalidatedLoader() {
		this.validateBuilt();
		return unvalidatedDataLoader;
	}

	@Override
	public final DataSaver<? super T> getUnvalidatedSaver() {
		this.validateBuilt();
		return unvalidatedDataSaver;
	}

	/**
	 * Sets the {@link DataAccessor} that is used to save and load values for this property.
	 * <p>
	 * The given {@link DataAccessor} is used like a black box. Since users of this property may
	 * want to control certain aspects related to the saving and loading of property values, it is
	 * usually recommended that the used {@link DataAccessor} does not handle the validation of
	 * values or deal with default or fallback values, but instead leaves these aspects to this
	 * property. If the given {@link DataAccessor} is able to return a loaded value of
	 * <code>null</code> instead of throwing a {@link MissingDataException}, this <code>null</code>
	 * value is processed like any other loaded value rather than being treated like missing data.
	 * <p>
	 * The data accessor has to be set exactly once before the property is {@link #build() built}.
	 * <p>
	 * As a typically useful convenience side effect, if the given data accessor is a
	 * {@link DataKeyAccessor} and no {@link #name(String) property name} has been set yet, this
	 * also sets the name of this property to the {@link DataKeyAccessor#getDataKey() data key} of
	 * the given {@link DataKeyAccessor}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param <A>
	 *            the type of the data accessor
	 * @param dataAccessor
	 *            the data accessor, not <code>null</code>
	 * @return this property
	 */
	@SuppressWarnings("unchecked")
	public final <P extends BasicProperty<T>, A extends DataLoader<? extends T> & DataSaver<? super T>> P dataAccessor(
			A dataAccessor
	) {
		this.validateNotBuilt();
		Validate.notNull(dataAccessor, "dataAccessor is null");
		Validate.State.isTrue(this.dataLoader == null,
				"Another DataAccessor has already been set!");
		assert this.dataSaver == null;
		this.dataLoader = dataAccessor;
		this.dataSaver = dataAccessor;
		if (this.name == null && dataAccessor instanceof DataKeyAccessor) {
			this.name(((DataKeyAccessor<?>) dataAccessor).getDataKey());
		}
		return (P) this;
	}

	/**
	 * Sets the {@link #dataAccessor(DataLoader) DataAccessor} to a {@link DataKeyAccessor} that
	 * uses the given {@link DataSerializer} to load and save the property value from the data at
	 * the specified data key.
	 * <p>
	 * At the same time, as a typically useful convenience side effect, this method also sets the
	 * {@link #name(String) name} of the property to the given data key, unless a name has already
	 * been set.
	 * <p>
	 * The data accessor and property name have to be set exactly once before the property is
	 * {@link #build() built}.
	 * 
	 * @param <P>
	 *            the type of this property
	 * @param dataKey
	 *            the data key, not <code>null</code> or empty
	 * @param serializer
	 *            the data serializer, not <code>null</code>
	 * @return this property
	 */
	public final <P extends BasicProperty<T>> P dataKeyAccessor(
			String dataKey,
			DataSerializer<@NonNull T> serializer
	) {
		// Note: This also automatically set the property name if no name has been set yet.
		return this.dataAccessor(new DataKeyAccessor<>(dataKey, serializer));
	}

	@Override
	public final void save(DataContainer dataContainer, @Nullable T value) {
		this.validateValue(value);
		this.saveUnvalidated(dataContainer, value);
	}

	private void saveUnvalidated(DataContainer dataContainer, @Nullable T value) {
		this.validateBuilt();
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
			throw new RuntimeException("Failed to save property '" + this.getName() + "': "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Saves the given value to the given {@link DataContainer}.
	 * <p>
	 * This is called by {@link #save(DataContainer, Object)} after the common validation and
	 * property logic has been performed.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @param value
	 *            the value, can be <code>null</code>
	 */
	private void saveValue(DataContainer dataContainer, @Nullable T value) {
		this.getSaver().save(dataContainer, value);
	}

	@Override
	public final T load(
			DataContainer dataContainer
	) throws MissingDataException, InvalidDataException {
		T value = this.loadUnvalidated(dataContainer);
		try {
			this.validateValue(value);
		} catch (Exception e) {
			throw new InvalidDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		}
		return value;
	}

	private T loadUnvalidated(
			DataContainer dataContainer
	) throws MissingDataException, InvalidDataException {
		this.validateBuilt();
		try {
			try {
				return this.loadValue(dataContainer); // Can be null
			} catch (MissingDataException e) {
				if (useDefaultIfMissing) {
					// This implies that there is a valid default value.
					return this.getDefaultValue();
				} else if (this.isNullable()) {
					return Unsafe.uncheckedNull();
				} else {
					throw e;
				}
			}
		} catch (MissingDataException e) {
			// Separate case to preserve the exception type.
			// Other exception types are not preserved, but callers can figure out the original
			// exception type via the exception cause.
			throw new MissingDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		} catch (InvalidDataException e) {
			throw new InvalidDataException(this.loadingFailedErrorMessage(e.getMessage()), e);
		}
	}

	private String loadingFailedErrorMessage(@Nullable String cause) {
		return "Failed to load property '" + this.getName() + "': " + cause;
	}

	/**
	 * Loads the value for this property from the given {@link DataContainer} using this property's
	 * {@link #getDataAccessor() DataAccessor}.
	 * <p>
	 * This method does not {@link #validateValue(Object) validate} the loaded value, nor does it
	 * handle default values.
	 * 
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 * @return the loaded value, can be <code>null</code>
	 * @throws MissingDataException
	 *             if the data for the value is missing
	 * @throws InvalidDataException
	 *             if the value cannot be loaded
	 */
	private T loadValue(DataContainer dataContainer) throws InvalidDataException {
		Validate.notNull(dataContainer, "dataContainer is null");
		return this.getLoader().load(dataContainer);
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
