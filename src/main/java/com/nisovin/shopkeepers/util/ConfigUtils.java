package com.nisovin.shopkeepers.util;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class ConfigUtils {

	// shared YAML config that gets reused
	private static final ThreadLocal<YamlConfiguration> YAML = ThreadLocal.withInitial(() -> new YamlConfiguration());

	public static Material loadMaterial(ConfigurationSection config, String key) {
		return loadMaterial(config, key, false);
	}

	public static Material loadMaterial(ConfigurationSection config, String key, boolean checkLegacy) {
		String materialName = config.getString(key); // note: takes defaults into account
		if (materialName == null) return null;
		Material material = Material.matchMaterial(materialName);
		if (material == null && checkLegacy) {
			// check for legacy material:
			String legacyMaterialName = Material.LEGACY_PREFIX + materialName;
			material = Material.matchMaterial(legacyMaterialName);
		}
		return material;
	}

	// the given top section itself does not get converted
	public static void convertSectionsToMaps(ConfigurationSection section) {
		for (Entry<String, Object> entry : section.getValues(false).entrySet()) {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				section.set(entry.getKey(), innerSectionMap);
				convertSectionsToMaps(innerSectionMap);
			}
		}
	}

	public static String yamlLineBreak() {
		return "\n"; // YAML used unix line breaks by default
	}

	public static void convertSectionsToMaps(Map<String, Object> sectionMap) {
		for (Entry<String, Object> entry : sectionMap.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof ConfigurationSection) {
				// recursively replace sections with maps:
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				entry.setValue(innerSectionMap);
				convertSectionsToMaps(innerSectionMap);
			}
		}
	}

	private static final String YAML_OUTPUT_KEY = "yaml-output";

	public static String[] getYAMLOutput(ConfigurationSerializable serializable) {
		return getYAMLOutput(serializable.serialize());
	}

	public static String[] getYAMLOutput(Object serializedObject) {
		YamlConfiguration yaml = YAML.get(); // shared yaml config
		yaml.set(YAML_OUTPUT_KEY, serializedObject);
		String configOutput = yaml.saveToString();
		yaml.set(YAML_OUTPUT_KEY, null);
		return configOutput.split(yamlLineBreak());
	}

	public static void clearConfigSection(ConfigurationSection configSection) {
		if (configSection == null) return;
		for (String key : configSection.getKeys(false)) {
			configSection.set(key, null);
		}
	}

	private ConfigUtils() {
	}
}
