package com.nisovin.shopkeepers.util;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class ConfigUtils {

	// Shared YAML config that gets reused:
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

	// The given top section itself does not get converted.
	public static void convertSectionsToMaps(ConfigurationSection section) {
		for (Entry<String, Object> entry : section.getValues(false).entrySet()) {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// Recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				section.set(entry.getKey(), innerSectionMap);
				convertSectionsToMaps(innerSectionMap);
			}
		}
	}

	public static void convertSectionsToMaps(Map<String, Object> sectionMap) {
		for (Entry<String, Object> entry : sectionMap.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// Recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				entry.setValue(innerSectionMap);
				convertSectionsToMaps(innerSectionMap);
			}
		}
	}

	public static void clearConfigSection(ConfigurationSection configSection) {
		if (configSection == null) return;
		for (String key : configSection.getKeys(false)) {
			configSection.set(key, null);
		}
	}

	// Not null, may be empty.
	public static String toYaml(String key, ConfigurationSerializable serializable) {
		if (serializable == null) return "";
		return toYaml(key, serializable.serialize());
	}

	// Not null, may be empty.
	public static String toYaml(String key, Object serializedObject) {
		YamlConfiguration yamlConfig = YAML_CONFIG.get(); // Shared YAML config
		yamlConfig.set(key, serializedObject);
		String yaml = yamlConfig.saveToString();
		yamlConfig.set(key, null);
		return yaml;
	}

	public static String yamlNewLine() {
		return "\n"; // YAML uses Unix line breaks by default
	}

	public static String[] splitYamlLines(String yaml) {
		Validate.notNull(yaml, "yaml is null");
		return yaml.split(yamlNewLine());
	}

	private ConfigUtils() {
	}
}
