package com.nisovin.shopkeepers.config.migration;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.Log;

/**
 * Migrates the config from version 0 (pre versioning) to version 1.
 * <p>
 * This previously converted legacy materials and item data values, as well as the shop creation item spawn egg to new
 * materials. This conversion is no longer supported. Instead we simply migrate all affected settings to their defaults.
 */
public class ConfigMigration1 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// pre 1.13 to 1.13:

		// shop creation item:
		// The conversion of spawn egg types is no longer supported.
		if (config.isSet("shop-creation-item-spawn-egg-entity-type")) {
			Log.info("  Migration of 'shop-creation-item-spawn-egg-entity-type' is no longer supported.");
			clear(config, "shop-creation-item-spawn-egg-entity-type");
		}
		migrateLegacyItemData(config, "shop-creation-item", "shop-creation-item-data", Material.VILLAGER_SPAWN_EGG);

		// name item:
		migrateLegacyItemData(config, "name-item", "name-item-data", Material.NAME_TAG);

		// chest item:
		migrateLegacyItemData(config, "chest-item", "chest-item-data", Material.CHEST);

		// delete item:
		migrateLegacyItemData(config, "delete-item", "delete-item-data", Material.BONE);

		// hire item:
		migrateLegacyItemData(config, "hire-item", "hire-item-data", Material.EMERALD);

		// currency item:
		migrateLegacyItemData(config, "currency-item", "currency-item-data", Material.EMERALD);

		// zero currency item:
		migrateLegacyItemData(config, "zero-currency-item", "zero-currency-item-data", Material.BARRIER);

		// high currency item:
		migrateLegacyItemData(config, "high-currency-item", "high-currency-item-data", Material.EMERALD_BLOCK);

		// high zero currency item:
		migrateLegacyItemData(config, "high-zero-currency-item", "high-zero-currency-item-data", Material.BARRIER);
	}

	// Returns true if the key got cleared.
	private static boolean clear(ConfigurationSection config, String key) {
		if (config.isSet(key)) {
			Log.info("  Removing '" + key + "' (previously '" + config.get(key, null) + "').");
			config.set(key, null);
			return true;
		}
		return false;
	}

	// This previously converted legacy material + data value to new material.
	// This conversion is no longer supported. Instead we migrate any unknown materials to their defaults.
	// Returns true if any migrations took place.
	private static boolean migrateLegacyItemData(ConfigurationSection config, String itemTypeKey, String itemDataKey, Material defaultType) {
		boolean migrated = false;

		// Migrate material, if present:
		String itemTypeName = config.getString(itemTypeKey, null);
		if (itemTypeName != null) {
			int itemData = config.getInt(itemDataKey, 0);
			// We no longer checks for legacy materials:
			Material itemType = ConfigUtils.loadMaterial(config, itemTypeKey);
			if (itemType == null) {
				// Fallback to default:
				Log.info("  Migration of '" + itemTypeKey + "' from type '" + itemTypeName + "' and data value '" + itemData
						+ "' is no longer supported. Falling back to default type '" + defaultType.name() + "'.");
				config.set(itemTypeKey, defaultType.name());
				migrated = true;
			}
		}

		// Remove data value from config:
		if (clear(config, itemDataKey)) {
			migrated = true;
		}
		return migrated;
	}
}
