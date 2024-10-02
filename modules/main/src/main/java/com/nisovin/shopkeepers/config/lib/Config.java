package com.nisovin.shopkeepers.config.lib;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.lib.annotation.Colored;
import com.nisovin.shopkeepers.config.lib.annotation.Uncolored;
import com.nisovin.shopkeepers.config.lib.annotation.WithDefaultValueType;
import com.nisovin.shopkeepers.config.lib.annotation.WithValueType;
import com.nisovin.shopkeepers.config.lib.annotation.WithValueTypeProvider;
import com.nisovin.shopkeepers.config.lib.setting.FieldSetting;
import com.nisovin.shopkeepers.config.lib.setting.Setting;
import com.nisovin.shopkeepers.config.lib.value.DefaultValueTypes;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.config.lib.value.ValueTypeProvider;
import com.nisovin.shopkeepers.config.lib.value.ValueTypeRegistry;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringListValue;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringValue;
import com.nisovin.shopkeepers.config.lib.value.types.StringListValue;
import com.nisovin.shopkeepers.config.lib.value.types.StringValue;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class Config {

	// Custom default value types specified by annotations:
	// This is lazily setup if required during settings setup.
	private @Nullable ValueTypeRegistry customDefaultValueTypes = null;

	// Lazily setup cache of all settings:
	private @Nullable Map<String, FieldSetting<?>> settings = null;
	private @Nullable Collection<? extends FieldSetting<?>> settingsView = null;

	protected Config() {
	}

	/**
	 * A short prefix for log messages related to this config.
	 * 
	 * @return the log prefix, not <code>null</code>, can be empty
	 */
	public String getLogPrefix() {
		return "Config: ";
	}

	// SETTINGS SETUP

	private void setupSettings() {
		Map<String, FieldSetting<?>> settings = this.settings;
		if (settings != null) {
			return; // Already setup
		}
		assert settingsView == null;

		settings = new LinkedHashMap<>();
		this.settings = settings;
		this.settingsView = Collections.unmodifiableCollection(settings.values());
		for (Field field : CollectionUtils.toIterable(this.streamSettingFields())) {
			String configKey = this.getConfigKey(field);
			ValueType<?> valueType = this.setupValueType(field);
			assert valueType != null;
			FieldSetting<?> setting = new FieldSetting<>(this, field, configKey, valueType);
			settings.put(configKey, setting);
		}

		// The custom default value types are only required during the setup:
		customDefaultValueTypes = null;
	}

	private Stream<Field> streamSettingFields() {
		Class<?> configClass = this.getClass();
		Stream<Field> settings = this.streamSettingFields(configClass);

		// Append setting fields of parent config classes (allows for composition of config
		// classes):
		// We stop once we reach this class in the type hierarchy:
		Class<?> parentClass = Unsafe.assertNonNull(configClass.getSuperclass());
		while (parentClass != Config.class) {
			settings = Stream.concat(settings, this.streamSettingFields(parentClass));
			parentClass = configClass.getSuperclass();
			assert parentClass != null;
		}
		return settings;
	}

	private final Stream<Field> streamSettingFields(Class<?> configClass) {
		List<Field> fields = Arrays.asList(configClass.getDeclaredFields());
		return fields.stream().filter(field -> {
			// Filter fields:
			if (field.isSynthetic()) return false;
			if (Modifier.isFinal(field.getModifiers())) return false;
			if (!this.isSetting(field)) return false;
			return true;
		});
	}

	/**
	 * This can be used to exclude fields from the settings.
	 * <p>
	 * By default, all public fields are included. Fields can also be declared in parent config
	 * classes.
	 * <p>
	 * Synthetic and final fields are always excluded.
	 * 
	 * @param field
	 *            the field
	 * @return <code>true</code> if the field is a setting, <code>false</code> otherwise
	 */
	protected boolean isSetting(Field field) {
		if (!Modifier.isPublic(field.getModifiers())) {
			return false;
		}
		return true;
	}

	// TODO Support for sections, e.g. by inner classes?
	// TODO Support @Key annotation.
	protected String getConfigKey(Field field) {
		return ConfigHelper.toConfigKey(field.getName());
	}

	// VALUE TYPES

	// Assert: Does not return null.
	protected <T> ValueType<T> setupValueType(Field field) {
		ValueType<T> valueType = this.getValueTypeByAnnotation(field);
		if (valueType != null) return valueType;

		valueType = this.getValueTypeByColoredAnnotation(field);
		if (valueType != null) return valueType;

		valueType = this.getValueTypeByUncoloredAnnotation(field);
		if (valueType != null) return valueType;

		valueType = this.getValueTypeByCustomDefaults(field);
		if (valueType != null) return valueType;

		Type fieldType = field.getGenericType();
		valueType = DefaultValueTypes.get(fieldType);
		if (valueType != null) return valueType;

		// ValueType could not be determined:
		String configKey = this.getConfigKey(field);
		throw new IllegalStateException("Setting '" + configKey + "' is of unsupported type: "
				+ fieldType.getTypeName());
	}

	@SuppressWarnings("unchecked")
	protected final <T> @Nullable ValueType<T> getValueTypeByAnnotation(Field field) {
		WithValueType valueTypeAnnotation = field.getAnnotation(WithValueType.class);
		if (valueTypeAnnotation != null) {
			Class<? extends ValueType<?>> valueTypeClass = valueTypeAnnotation.value();
			assert valueTypeClass != null;
			return (ValueType<T>) instantiateValueType(valueTypeClass);
		}
		return null;
	}

	private static ValueType<?> instantiateValueType(
			Class<? extends ValueType<?>> valueTypeClass
	) {
		assert valueTypeClass != null;
		try {
			return valueTypeClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not instantiate ValueType: "
					+ valueTypeClass.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	protected final <T> @Nullable ValueType<T> getValueTypeByColoredAnnotation(Field field) {
		Colored coloredAnnotation = field.getAnnotation(Colored.class);
		if (coloredAnnotation != null) {
			Type fieldType = field.getGenericType();
			if (fieldType == String.class) {
				return (ValueType<T>) ColoredStringValue.INSTANCE;
			} else if (ColoredStringListValue.TYPE_PATTERN.matches(fieldType)) {
				return (ValueType<T>) ColoredStringListValue.INSTANCE;
			} else {
				throw new IllegalArgumentException(
						"The Colored annotation is not supported for settings of type "
								+ fieldType.getTypeName()
				);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected final <T> @Nullable ValueType<T> getValueTypeByUncoloredAnnotation(Field field) {
		Uncolored uncoloredAnnotation = field.getAnnotation(Uncolored.class);
		if (uncoloredAnnotation != null) {
			Type fieldType = field.getGenericType();
			if (fieldType == String.class) {
				return (ValueType<T>) StringValue.INSTANCE;
			} else if (ColoredStringListValue.TYPE_PATTERN.matches(fieldType)) {
				return (ValueType<T>) StringListValue.INSTANCE;
			} else {
				throw new IllegalArgumentException(
						"The Uncolored annotation is not supported for settings of type "
								+ fieldType.getTypeName()
				);
			}
		}
		return null;
	}

	protected final <T> @Nullable ValueType<T> getValueTypeByCustomDefaults(Field field) {
		this.setupCustomDefaultValueTypes(); // Lazy setup
		Type fieldType = field.getGenericType();
		return Unsafe.assertNonNull(customDefaultValueTypes).getValueType(fieldType);
	}

	private void setupCustomDefaultValueTypes() {
		if (this.customDefaultValueTypes != null) {
			return; // Already setup.
		}

		this.customDefaultValueTypes = new ValueTypeRegistry();
		Class<?> configClass = this.getClass();
		this.setupCustomDefaultValueTypes(configClass);

		// Also take into account custom default value types specified in parent classes:
		Class<?> parentClass = Unsafe.assertNonNull(configClass.getSuperclass());
		while (parentClass != Config.class) {
			this.setupCustomDefaultValueTypes(parentClass);
			parentClass = configClass.getSuperclass();
			assert parentClass != null;
		}
	}

	private void setupCustomDefaultValueTypes(Class<?> configClass) {
		ValueTypeRegistry customDefaultValueTypes = Unsafe.assertNonNull(this.customDefaultValueTypes);
		// WithDefaultValueType annotations:
		WithDefaultValueType[] defaultValueTypeAnnotations = configClass.getAnnotationsByType(WithDefaultValueType.class);
		assert defaultValueTypeAnnotations != null;
		for (WithDefaultValueType defaultValueTypeAnnotation : defaultValueTypeAnnotations) {
			Class<?> fieldType = defaultValueTypeAnnotation.fieldType();
			if (customDefaultValueTypes.hasCachedValueType(fieldType)) {
				// Only the first encountered default value type specification is used:
				continue;
			}

			Class<? extends ValueType<?>> valueTypeClass = defaultValueTypeAnnotation.valueType();
			assert valueTypeClass != null;
			ValueType<?> valueType = instantiateValueType(valueTypeClass);
			assert valueType != null; // Else: Exception is thrown.
			customDefaultValueTypes.register(fieldType, valueType);
		}

		// WithValueTypeProvider annotations:
		WithValueTypeProvider[] valueTypeProviderAnnotations = configClass.getAnnotationsByType(WithValueTypeProvider.class);
		assert valueTypeProviderAnnotations != null;
		for (WithValueTypeProvider valueTypeProviderAnnotation : valueTypeProviderAnnotations) {
			Class<? extends ValueTypeProvider> valueTypeProviderClass = valueTypeProviderAnnotation.value();
			assert valueTypeProviderClass != null;
			ValueTypeProvider valueTypeProvider = instantiateValueTypeProvider(valueTypeProviderClass);
			assert valueTypeProvider != null; // Else: Exception is thrown.
			customDefaultValueTypes.register(valueTypeProvider);
		}
	}

	private static ValueTypeProvider instantiateValueTypeProvider(
			Class<? extends ValueTypeProvider> valueTypeProviderClass
	) {
		assert valueTypeProviderClass != null;
		try {
			return valueTypeProviderClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not instantiate ValueTypeProvider: "
					+ valueTypeProviderClass.getName(), e);
		}
	}

	// SETTINGS

	/**
	 * Gets the settings.
	 * 
	 * @return an unmodifiable view on the settings
	 */
	public final Collection<? extends Setting<?>> getSettings() {
		this.setupSettings();
		return Unsafe.assertNonNull(settingsView);
	}

	// Returns null if there is no setting for this config key:
	protected final @Nullable Setting<?> getSetting(String configKey) {
		this.setupSettings();
		return Unsafe.assertNonNull(settings).get(configKey);
	}

	// SAVING

	public void save(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		for (Setting<?> setting : this.getSettings()) {
			this.saveSetting(dataContainer, setting);
		}
	}

	protected <T> void saveSetting(DataContainer dataContainer, Setting<T> setting) {
		assert setting.getConfig() == this;
		String configKey = setting.getConfigKey();
		ValueType<T> valueType = setting.getValueType();
		@Nullable T value = setting.getValue();
		valueType.save(dataContainer, configKey, value);
	}

	// LOADING

	public void load(ConfigData configData) throws ConfigLoadException {
		Validate.notNull(configData, "configData is null");
		for (Setting<?> setting : this.getSettings()) {
			this.loadSetting(configData, setting);
		}
		this.validateSettings();
	}

	protected <T> void loadSetting(
			ConfigData configData,
			Setting<T> setting
	) throws ConfigLoadException {
		assert setting.getConfig() == this;
		String configKey = setting.getConfigKey();
		ValueType<T> valueType = setting.getValueType();
		try {
			@Nullable T value;

			// Handle missing value:
			if (!configData.contains(configKey)) {
				// We use the default value, if there is one:
				value = this.getDefaultValue(configData, setting); // Can be null
				this.onValueMissing(configData, setting, value);
			} else {
				// Load value:
				value = valueType.load(configData, configKey);
				assert value != null; // We expect an exception if the value cannot be loaded
			}

			// Apply value:
			if (value != null) {
				setting.setValue(value);
			} // Else: Retain previous value.
		} catch (ValueLoadException e) {
			this.onValueLoadException(configData, setting, e);
		}
	}

	protected <T> void onValueMissing(
			ConfigData configData,
			Setting<T> setting,
			@Nullable T defaultValue
	) throws ConfigLoadException {
		String configKey = setting.getConfigKey();
		if (defaultValue == null) {
			Log.warning(this.msgMissingValue(configKey));
		} else {
			Log.warning(this.msgUsingDefaultForMissingValue(configKey, defaultValue));
		}
	}

	protected String msgMissingValue(String configKey) {
		return this.getLogPrefix() + "Missing config entry: " + configKey;
	}

	protected String msgUsingDefaultForMissingValue(String configKey, Object defaultValue) {
		return this.getLogPrefix() + "Using default value for missing config entry: " + configKey;
	}

	protected <T> void onValueLoadException(
			ConfigData configData,
			Setting<T> setting,
			ValueLoadException e
	) throws ConfigLoadException {
		String configKey = setting.getConfigKey();
		Log.warning(this.msgValueLoadException(configKey, e));
		for (String extraMessage : e.getExtraMessages()) {
			Log.warning(this.getLogPrefix() + extraMessage);
		}
	}

	protected String msgValueLoadException(String configKey, ValueLoadException e) {
		return this.getLogPrefix() + "Could not load setting '" + configKey + "': "
				+ e.getMessage();
	}

	/**
	 * Validates the values of settings once all settings have been loaded.
	 */
	protected void validateSettings() {
	}

	// DEFAULT VALUES

	// Checks whether there are default values available.
	// This has to be consistent with #getDefaultValue.
	protected boolean hasDefaultValues(ConfigData configData) {
		return (configData.getDefaults() != null);
	}

	// Returns null if there is no default value.
	@SuppressWarnings("unchecked")
	protected <T> @Nullable T getDefaultValue(ConfigData configData, Setting<T> setting) {
		assert setting.getConfig() == this;
		DataContainer defaults = configData.getDefaults();
		if (defaults == null) {
			// No default config values available.
			return null;
		}

		String configKey = setting.getConfigKey();
		ValueType<?> valueType = setting.getValueType();

		// Load default value:
		try {
			// Note: This can return null if the config data does not provide a default value for
			// this setting.
			return (T) valueType.load(defaults, configKey);
		} catch (ValueLoadException e) {
			Log.warning(this.msgDefaultValueLoadException(configKey, e));
			return null;
		}
	}

	protected String msgDefaultValueLoadException(String configKey, ValueLoadException e) {
		return this.getLogPrefix() + "Could not load default value for setting '" + configKey
				+ "': " + e.getMessage();
	}

	/**
	 * Inserts the default values for missing settings into the given {@link ConfigData}.
	 * 
	 * @param configData
	 *            the config data, not <code>null</code>
	 * @return <code>true</code> if any default values have been inserted
	 */
	protected boolean insertMissingDefaultValues(ConfigData configData) {
		Validate.notNull(configData, "configData is null");
		if (!this.hasDefaultValues(configData)) {
			// No default config values available.
			return false;
		}

		// Initialize missing settings with their default value:
		boolean configChanged = false;
		for (Setting<?> setting : this.getSettings()) {
			if (this.insertMissingDefaultValue(configData, setting)) {
				configChanged = true;
			}
		}
		return configChanged;
	}

	// Returns true if the default value got inserted.
	protected <T> boolean insertMissingDefaultValue(ConfigData configData, Setting<T> setting) {
		assert setting.getConfig() == this;
		assert this.hasDefaultValues(configData);
		String configKey = setting.getConfigKey();
		if (configData.contains(configKey)) return false; // Not missing.

		Log.warning(this.msgInsertingDefault(configKey));

		// Get default value:
		@Nullable T defaultValue = this.getDefaultValue(configData, setting);
		if (defaultValue == null) {
			Log.warning(this.msgMissingDefault(configKey));
			return false;
		}

		// Save default value to config:
		ValueType<T> valueType = setting.getValueType();
		valueType.save(configData, configKey, defaultValue);
		return true;
	}

	protected String msgInsertingDefault(String configKey) {
		return this.getLogPrefix() + "Inserting default value for missing config entry: "
				+ configKey;
	}

	protected String msgMissingDefault(String configKey) {
		return this.getLogPrefix() + "Missing default value for setting: " + configKey;
	}

	// CONVENIENCE HELPERS

	/**
	 * Translates {@code &}-based color codes to Minecraft's {@code §}-based color codes.
	 * 
	 * @param text
	 *            the text with {@code &}-based color codes
	 * @return the text with Minecraft's color codes, or <code>null</code> if the given text is
	 *         <code>null</code>
	 */
	protected static String c(String text) {
		return TextUtils.colorize(text);
	}

	/**
	 * Translates {@code &}-based color codes to Minecraft's {@code §}-based color codes.
	 * 
	 * @param list
	 *            the texts with {@code &}-based color codes, not <code>null</code>
	 * @return a new list containing the corresponding texts with Minecraft's color codes
	 */
	protected static List<String> c(List<? extends String> texts) {
		return TextUtils.colorize(texts);
	}
}
