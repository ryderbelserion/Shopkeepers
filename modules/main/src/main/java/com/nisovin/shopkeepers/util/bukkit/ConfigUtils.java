package com.nisovin.shopkeepers.util.bukkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.LogDetectionHandler;

public final class ConfigUtils {

	// Shared and reused YAML config:
	private static final ThreadLocal<YamlConfiguration> YAML_CONFIG = ThreadLocal.withInitial(
			ConfigUtils::newYamlConfig
	);

	private static final LogDetectionHandler ERROR_DETECTION_HANDLER = new LogDetectionHandler();
	static {
		ERROR_DETECTION_HANDLER.setLevel(Level.SEVERE);
	}

	/**
	 * Creates a new {@link YamlConfiguration} with some common default setup applied.
	 * 
	 * @return the new {@link YamlConfiguration}
	 */
	public static YamlConfiguration newYamlConfig() {
		YamlConfiguration config = new YamlConfiguration();
		// We (e.g. inside of language files), as well as the Bukkit server itself (e.g. for the
		// serialization of attributes inside ItemMeta), can use keys that may contain dots. In
		// order to prevent Bukkit from interpreting (and loading) these dots as configuration
		// sections, we replace Bukkit's default configuration section path separator from dot to
		// something else.
		disablePathSeparator(config);
		return config;
	}

	public static void disablePathSeparator(Configuration config) {
		// Set to some highly unusual value:
		config.options().pathSeparator('\0');
	}

	public static Map<String, Object> getValues(ConfigurationSection section) {
		return section.getValues(false);
	}

	// The given root config section itself is not converted.
	public static void convertSubSectionsToMaps(ConfigurationSection rootSection) {
		rootSection.getValues(false).forEach((key, value) -> {
			assert key != null;
			if (value instanceof ConfigurationSection) {
				ConfigurationSection section = (ConfigurationSection) value;
				// Recursively replace config sections with maps:
				Map<String, Object> innerSectionMap = getValues(section);
				convertSectionsToMaps(innerSectionMap);
				rootSection.set(key, innerSectionMap);
			}
		});
	}

	// Also converts the given root config section.
	public static Map<String, Object> convertSectionsToMaps(ConfigurationSection rootSection) {
		Map<String, Object> sectionMap = getValues(rootSection);
		convertSectionsToMaps(sectionMap);
		return sectionMap;
	}

	// This requires the given Map to be modifiable.
	public static void convertSectionsToMaps(Map<? extends String, Object> rootMap) {
		rootMap.entrySet().forEach(entry -> {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				ConfigurationSection section = (ConfigurationSection) value;
				// Recursively replace config sections with maps:
				Map<String, Object> innerSectionMap = getValues(section);
				convertSectionsToMaps(innerSectionMap);
				entry.setValue(innerSectionMap);
			}
		});
	}

	public static void clearConfigSection(ConfigurationSection configSection) {
		Validate.notNull(configSection, "configSection is null");
		configSection.getKeys(false).forEach(key -> {
			assert key != null;
			configSection.set(key, null);
		});
	}

	public static void setAll(ConfigurationSection configSection, Map<?, ?> map) {
		Validate.notNull(configSection, "configSection is null");
		Validate.notNull(map, "map is null");
		map.forEach((key, value) -> {
			String stringKey = StringUtils.toStringOrNull(key);
			if (stringKey != null) {
				configSection.set(stringKey, value);
			}
		});
	}

	// Mimics Bukkit's serialization. Includes the type key of the given ConfigurationSerializable.
	public static Map<String, Object> serialize(ConfigurationSerializable serializable) {
		Validate.notNull(serializable, "serializable is null");
		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put(
				ConfigurationSerialization.SERIALIZED_TYPE_KEY,
				ConfigurationSerialization.getAlias(serializable.getClass())
		);
		dataMap.putAll(serializable.serialize());
		return dataMap;
	}

	// Expects the Map to contain a type key, and any inner serializable data to already be
	// deserialized.
	public static <T extends ConfigurationSerializable> @Nullable T deserialize(
			@Nullable Map<? extends @Nullable String, ?> dataMap
	) {
		if (dataMap == null) return null;
		try {
			return Unsafe.cast(ConfigurationSerialization.deserializeObject(
					Unsafe.castNonNull(dataMap)
			));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Could not deserialize object", ex);
		}
	}

	public static Map<String, Object> serializeDeeply(
			ConfigurationSerializable serializable
	) {
		Validate.notNull(serializable, "serializable is null");
		Map<String, Object> dataMap = serialize(serializable);
		serializeDeeply(dataMap);
		return dataMap;
	}

	// This deeply and recursively replaces all serializable elements, as well as
	// ConfigurationSections, in the given Map with their respective serializations. The given Map
	// is expected to be modifiable. But since the inner Maps may be immutable, they may need to be
	// copied.
	public static void serializeDeeply(@Nullable Map<?, Object> dataMap) {
		if (dataMap == null) return;
		dataMap.entrySet().forEach(entry -> {
			Object value = entry.getValue();
			if (value instanceof Map) {
				// The Map may be unmodifiable. But since we may need to recursively replace its
				// entries, we need to copy it.
				Map<?, Object> innerMap = new LinkedHashMap<>((Map<?, @NonNull ?>) value);
				serializeDeeply(innerMap);
				entry.setValue(innerMap);
			} else if (value instanceof ConfigurationSection) {
				ConfigurationSection section = (ConfigurationSection) value;
				Map<String, Object> innerSectionMap = getValues(section);
				serializeDeeply(innerSectionMap);
				entry.setValue(innerSectionMap);
			} else if (value instanceof ConfigurationSerializable) {
				ConfigurationSerializable serializable = (ConfigurationSerializable) value;
				Map<String, Object> innerSerializableData = serializeDeeply(serializable);
				entry.setValue(innerSerializableData);
			}
		});
	}

	// This does not store the given data under any key, but inserts it into the top-level map of a
	// YamlConfiguration.
	// Does not return null, even if the given Map is null.
	// Note: If the given map is the data of a serialized ConfigurationSerializable, and it includes
	// its serialized type key, the produced Yaml output may not be loadable again as a
	// YamlConfiguration, because it will deserialize as a ConfigurationSerializable instead of a
	// Map.
	public static String toFlatConfigYaml(Map<?, ?> map) {
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			setAll(yamlConfig, map);
			return yamlConfig.saveToString();
		} finally {
			clearConfigSection(yamlConfig);
		}
	}

	// Does not return null. Returns an empty String if the object is null.
	public static String toConfigYaml(String key, @Nullable Object object) {
		if (object == null) return "";
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			yamlConfig.set(key, object);
			return yamlConfig.saveToString();
		} finally {
			yamlConfig.set(key, null);
		}
	}

	public static String toConfigYamlWithoutTrailingNewline(String key, Object object) {
		return StringUtils.stripTrailingNewlines(toConfigYaml(key, object));
	}

	// The input is expected to be a serialized config Map.
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T fromConfigYaml(@Nullable String yamlConfigString, String key) {
		if (StringUtils.isEmpty(yamlConfigString)) return null;
		assert yamlConfigString != null;
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			yamlConfig.loadFromString(yamlConfigString);
			return (T) yamlConfig.get(key);
		} catch (InvalidConfigurationException e) {
			return null;
		} finally {
			clearConfigSection(yamlConfig);
		}
	}

	// TODO Hack to detect issues during the deserialization of ConfigurationSerializables. Bukkit
	// does not throw exceptions in those cases, but instead only logs an error and then
	// deserializes the value as null.
	// When an error is detected, we wrap it into an InvalidConfigurationException.
	public static void loadConfigSafely(
			FileConfiguration config,
			String contents
	) throws InvalidConfigurationException {
		Validate.notNull(config, "config is null");
		// Get the logger that is used during the deserialization of ConfigurationSerializables:
		Logger configSerializationLogger = Logger.getLogger(ConfigurationSerialization.class.getName());

		// Capture the current logger state:
		@NonNull Handler[] handlers = configSerializationLogger.getHandlers();
		boolean useParent = configSerializationLogger.getUseParentHandlers();
		try {
			// Disable logging:
			for (Handler handler : handlers) {
				configSerializationLogger.removeHandler(handler);
			}
			configSerializationLogger.setUseParentHandlers(false);

			// Register our own error detection handler:
			configSerializationLogger.addHandler(ERROR_DETECTION_HANDLER);

			// Load the config:
			config.loadFromString(contents);

			// Check if we detected an error:
			LogRecord error = ERROR_DETECTION_HANDLER.getLastLogRecord();
			if (error != null) {
				throw new InvalidConfigurationException(error.getMessage(), error.getThrown());
			}
		} finally {
			// Reset the error detection handler:
			ERROR_DETECTION_HANDLER.reset();

			// Restore the previous logger state:
			configSerializationLogger.removeHandler(ERROR_DETECTION_HANDLER);
			for (Handler handler : handlers) {
				configSerializationLogger.addHandler(handler);
			}
			configSerializationLogger.setUseParentHandlers(useParent);
		}
	}

	private ConfigUtils() {
	}
}
