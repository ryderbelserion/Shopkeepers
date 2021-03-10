package com.nisovin.shopkeepers.util;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class ConfigUtils {

	// Shared and reused YAML config:
	private static final ThreadLocal<YamlConfiguration> YAML_CONFIG = ThreadLocal.withInitial(() -> new YamlConfiguration());

	public static Material loadMaterial(ConfigurationSection config, String key) {
		String materialName = config.getString(key); // Note: Takes defaults into account.
		if (materialName == null) return null;
		Material material = ItemUtils.parseMaterial(materialName); // Can be null
		if (material != null && material.isLegacy()) {
			return null;
		}
		return material;
	}

	// The given top level section itself is not converted.
	public static void convertSectionsToMaps(ConfigurationSection section) {
		section.getValues(false).entrySet().forEach(entry -> {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// Recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				convertSectionsToMaps(innerSectionMap);
				section.set(entry.getKey(), innerSectionMap);
			}
		});
	}

	// This requires the given Map to be modifiable.
	public static void convertSectionsToMaps(Map<String, Object> sectionMap) {
		sectionMap.entrySet().forEach(entry -> {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// Recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				convertSectionsToMaps(innerSectionMap);
				entry.setValue(innerSectionMap);
			}
		});
	}

	public static void clearConfigSection(ConfigurationSection configSection) {
		if (configSection == null) return;
		configSection.getKeys(false).forEach(key -> {
			configSection.set(key, null);
		});
	}

	// Not null.
	public static String toYaml(ConfigurationSerializable serializable) {
		return toYaml((serializable != null) ? serializable.serialize() : null);
	}

	// Not null.
	public static String toYaml(Map<String, Object> map) {
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			if (map != null) {
				map.entrySet().forEach(entry -> {
					yamlConfig.set(entry.getKey(), entry.getValue());
				});
			}
			return yamlConfig.saveToString();
		} finally {
			clearConfigSection(yamlConfig);
		}
	}

	// Not null.
	public static String toYaml(String key, ConfigurationSerializable serializable) {
		return toYaml(key, (serializable != null) ? serializable.serialize() : null);
	}

	// Not null.
	public static String toYaml(String key, Object serializedObject) {
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			yamlConfig.set(key, serializedObject);
			return yamlConfig.saveToString();
		} finally {
			yamlConfig.set(key, null);
		}
	}

	public static String yamlNewline() {
		return "\n"; // YAML uses Unix line breaks by default
	}

	public static String[] splitYamlLines(String yaml) {
		Validate.notNull(yaml, "yaml String is null");
		return yaml.split(yamlNewline());
	}

	private ConfigUtils() {
	}
}
