package com.nisovin.shopkeepers.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;

public class ConfigHelper {

	private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("([A-Z][a-z]+)");

	private ConfigHelper() {
	}

	public static String toConfigKey(String fieldName) {
		return CONFIG_KEY_PATTERN.matcher(fieldName).replaceAll("-$1").toLowerCase(Locale.ROOT);
	}

	public static Object loadConfigValue(Configuration config, String configKey, Set<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType) {
		if (typeClass == String.class || typeClass == Text.class) {
			String string = config.getString(configKey);
			// Colorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				string = TextUtils.colorize(string);
			}
			if (typeClass == Text.class) {
				return Text.parse(string);
			} else {
				return string;
			}
		} else if (typeClass == int.class) {
			return config.getInt(configKey);
		} else if (typeClass == short.class) {
			return (short) config.getInt(configKey);
		} else if (typeClass == boolean.class) {
			return config.getBoolean(configKey);
		} else if (typeClass == Material.class) {
			// This assumes that legacy item conversion has already been performed
			Material material = ConfigUtils.loadMaterial(config, configKey);
			if (material == null) {
				Log.warning("Config: Unknown material for config entry '" + configKey + "': " + config.get(configKey));
				Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
			}
			return material;
		} else if (typeClass == ItemData.class) {
			ItemData itemData = loadItemData(config.get(configKey), configKey);
			// Normalize to not null:
			if (itemData == null) {
				itemData = new ItemData(Material.AIR);
			}
			return itemData;
		} else if (typeClass == List.class) {
			if (genericType == String.class || genericType == Text.class) {
				List<String> stringList = config.getStringList(configKey);
				// Colorize, if not exempted:
				if (!noColorConversionKeys.contains(configKey)) {
					stringList = TextUtils.colorize(stringList);
				}
				if (genericType == Text.class) {
					return Text.parse(stringList);
				} else {
					return stringList;
				}
			} else if (genericType == ItemData.class) {
				List<?> list = config.getList(configKey, Collections.emptyList());
				List<ItemData> itemDataList = new ArrayList<>(list.size());
				int index = 0;
				for (Object entry : list) {
					index += 1;
					ItemData itemData = loadItemData(entry, configKey + "[" + index + "]");
					if (itemData != null) {
						itemDataList.add(itemData);
					}
				}
				return itemDataList;
			} else {
				throw new IllegalStateException("Unsupported config setting list type: " + genericType.getName());
			}
		}
		throw new IllegalStateException("Unsupported config setting type: " + typeClass.getName());
	}

	public static ItemData loadItemData(Object dataObject, String configEntryIdentifier) {
		ItemData itemData = ItemData.deserialize(dataObject, (warning) -> {
			Log.warning("Config: Couldn't load item data for config entry '" + configEntryIdentifier + "': " + warning);
			if (warning.contains("Unknown item type")) { // TODO this is ugly
				Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
			}
		});
		return itemData;
	}

	public static void setConfigValue(Configuration config, String configKey, Set<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType, Object value) {
		if (value == null) {
			// Remove value:
			config.set(configKey, null);
			return;
		}

		if (typeClass == Material.class) {
			config.set(configKey, ((Material) value).name());
		} else if (typeClass == String.class || typeClass == Text.class) {
			String stringValue;
			if (typeClass == Text.class) {
				stringValue = ((Text) value).toPlainFormatText();
			} else {
				stringValue = (String) value;
			}
			// Decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize(stringValue);
			}
			config.set(configKey, value);
		} else if (typeClass == List.class && (genericType == String.class || genericType == Text.class)) {
			List<String> stringList;
			if (genericType == Text.class) {
				stringList = ((List<Text>) value).stream().map(Text::toPlainFormatText).collect(Collectors.toList());
			} else {
				stringList = (List<String>) value;
			}

			// Decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize(stringList);
			}
			config.set(configKey, value);
		} else if (typeClass == ItemData.class) {
			config.set(configKey, ((ItemData) value).serialize());
		} else {
			config.set(configKey, value);
		}
	}
}
