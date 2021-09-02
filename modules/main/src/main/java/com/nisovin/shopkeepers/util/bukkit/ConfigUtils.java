package com.nisovin.shopkeepers.util.bukkit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MapUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

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

	// Additional processing whenever we load deserialized item stacks from a config.
	public static ItemStack loadItemStack(ConfigurationSection config, String key) {
		// Note: Spigot creates Bukkit ItemStacks, whereas Paper automatically replaces the deserialized Bukkit
		// ItemStacks with CraftItemStacks. However, as long as the deserialized item stack is not compared directly to
		// an unmodifiable item stack (at least not without first being wrapped into an unmodifiable item stack itself),
		// and assuming that there are no inconsistencies in how CraftItemStacks and Bukkit ItemStacks are compared with
		// each other, this difference should not be relevant to us.
		ItemStack itemStack = config.getItemStack(key);

		// TODO SPIGOT-6716, PAPER-6437: The order of stored enchantments of enchanted books is not consistent. On
		// Paper, where the deserialized ItemStacks end up being CraftItemStacks, this difference in enchantment order
		// can cause issues when these deserialized item stacks are compared to other CraftItemStacks. Converting these
		// deserialized CraftItemStacks back to Bukkit ItemStacks ensures that the comparisons with other
		// CraftItemStacks ignore the enchantment order.
		if (itemStack != null && itemStack.getType() == Material.ENCHANTED_BOOK) {
			itemStack = ItemUtils.ensureBukkitItemStack(itemStack);
		}
		return itemStack;
	}

	public static UnmodifiableItemStack loadUnmodifiableItemStack(ConfigurationSection config, String key) {
		return UnmodifiableItemStack.of(loadItemStack(config, key));
	}

	// This creates a (shallow) copy of the Map.
	public static Map<String, Object> loadStringMap(Object dataObject) {
		if (dataObject instanceof Map) {
			return MapUtils.toStringMap((Map<?, ?>) dataObject);
		} else if (dataObject instanceof ConfigurationSection) {
			return ((ConfigurationSection) dataObject).getValues(false);
		} else {
			return null;
		}
	}

	// This returns the original Map if the given object is already a Map.
	public static Map<?, ?> loadMap(Object dataObject) {
		if (dataObject instanceof Map) {
			return (Map<?, ?>) dataObject;
		} else if (dataObject instanceof ConfigurationSection) {
			return ((ConfigurationSection) dataObject).getValues(false);
		} else {
			return null;
		}
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

	public static void setAll(ConfigurationSection configSection, Map<String, Object> map) {
		Validate.notNull(configSection, "configSection is null");
		if (map != null) {
			map.entrySet().forEach(entry -> {
				configSection.set(entry.getKey(), entry.getValue());
			});
		}
	}

	// Mimics Bukkit's serialization. Includes the type key of the given ConfigurationSerializable.
	public static Map<String, Object> serialize(ConfigurationSerializable serializable) {
		if (serializable == null) return null;
		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
		dataMap.putAll(serializable.serialize());
		return dataMap;
	}

	// Expects the Map to contain a type key, and any inner serializable data to already be deserialized.
	@SuppressWarnings("unchecked")
	public static <T extends ConfigurationSerializable> T deserialize(Map<String, Object> dataMap) {
		if (dataMap == null) return null;
		try {
			return (T) ConfigurationSerialization.deserializeObject(dataMap);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Could not deserialize object", ex);
		}
	}

	public static Map<String, Object> serializeDeeply(ConfigurationSerializable serializable) {
		Map<String, Object> dataMap = serialize(serializable); // Can be null
		serializeDeeply(dataMap);
		return dataMap;
	}

	// This deeply and recursively replaces all serializable elements, as well as ConfigurationSections, in the given
	// Map with their respective serializations. The given Map is expected to be modifiable. But since the inner Maps
	// may be immutable, they may need to be copied.
	public static void serializeDeeply(Map<?, Object> dataMap) {
		if (dataMap == null) return;
		dataMap.entrySet().forEach(entry -> {
			Object value = entry.getValue();
			if (value instanceof Map) {
				// The Map may be unmodifiable. But since we may need to recursively replace its entries, we need to
				// copy it.
				Map<?, Object> innerMap = new LinkedHashMap<>((Map<?, ?>) value);
				serializeDeeply(innerMap);
				entry.setValue(innerMap);
			} else if (value instanceof ConfigurationSection) {
				Map<String, Object> innerSectionMap = ((ConfigurationSection) value).getValues(false);
				serializeDeeply(innerSectionMap);
				entry.setValue(innerSectionMap);
			} else if (value instanceof ConfigurationSerializable) {
				Map<String, Object> innerSerializableData = serializeDeeply((ConfigurationSerializable) value);
				entry.setValue(innerSerializableData);
			}
		});
	}

	// This does not store the given data under any key, but inserts it into the top-level map of a YamlConfiguration.
	// Does not return null, even if the given Map is null.
	// Note: If the given map is the data of a serialized ConfigurationSerializable, and it includes its serialized type
	// key, the produced Yaml output may not be loadable again as a YamlConfiguration, because it will deserialize as a
	// ConfigurationSerializable instead of a Map.
	public static String toFlatConfigYaml(Map<String, Object> map) {
		YamlConfiguration yamlConfig = YAML_CONFIG.get();
		try {
			setAll(yamlConfig, map);
			return yamlConfig.saveToString();
		} finally {
			clearConfigSection(yamlConfig);
		}
	}

	// Does not return null. Returns an empty String if the object is null.
	public static String toConfigYaml(String key, Object object) {
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
	public static <T> T fromConfigYaml(String yamlConfigString, String key) {
		if (yamlConfigString == null) return null;
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

	private ConfigUtils() {
	}
}
