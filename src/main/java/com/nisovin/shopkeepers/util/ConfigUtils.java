package com.nisovin.shopkeepers.util;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigUtils {

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

	private ConfigUtils() {
	}
}
