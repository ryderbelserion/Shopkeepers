package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import org.bukkit.Material;

import com.nisovin.shopkeepers.util.bukkit.DataUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Migrates the config from version 0 (pre versioning) to version 1.
 * <p>
 * This previously converted legacy materials and item data values, as well as the shop creation
 * item spawn egg to new materials. This conversion is no longer supported. Instead, we simply
 * migrate all affected settings to their defaults.
 */
public class ConfigMigration1 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Pre 1.13 to 1.13:

		// Shop creation item:
		// The conversion of spawn egg types is no longer supported.
		if (configData.contains("shop-creation-item-spawn-egg-entity-type")) {
			Log.info("  Migration of 'shop-creation-item-spawn-egg-entity-type' is no longer supported.");
			removeSetting(configData, "shop-creation-item-spawn-egg-entity-type");
		}
		migrateLegacyItemData(
				configData,
				"shop-creation-item",
				"shop-creation-item-data",
				Material.VILLAGER_SPAWN_EGG
		);

		// Name item:
		migrateLegacyItemData(
				configData,
				"name-item",
				"name-item-data",
				Material.NAME_TAG
		);

		// Chest item:
		migrateLegacyItemData(
				configData,
				"chest-item",
				"chest-item-data",
				Material.CHEST
		);

		// Delete item:
		migrateLegacyItemData(
				configData,
				"delete-item",
				"delete-item-data",
				Material.BONE
		);

		// Hire item:
		migrateLegacyItemData(
				configData,
				"hire-item",
				"hire-item-data",
				Material.EMERALD
		);

		// Currency item:
		migrateLegacyItemData(
				configData,
				"currency-item",
				"currency-item-data",
				Material.EMERALD
		);

		// Zero currency item:
		migrateLegacyItemData(
				configData,
				"zero-currency-item",
				"zero-currency-item-data",
				Material.BARRIER
		);

		// High currency item:
		migrateLegacyItemData(
				configData,
				"high-currency-item",
				"high-currency-item-data",
				Material.EMERALD_BLOCK
		);

		// High zero currency item:
		migrateLegacyItemData(
				configData,
				"high-zero-currency-item",
				"high-zero-currency-item-data",
				Material.BARRIER
		);
	}

	// This previously converted legacy material + data value to new material.
	// This conversion is no longer supported. Instead, we migrate any unknown materials to their
	// defaults.
	// Returns true if any migrations took place.
	private static boolean migrateLegacyItemData(
			DataContainer configData,
			String itemTypeKey,
			String itemDataKey,
			Material defaultType
	) {
		boolean migrated = false;

		// Migrate material, if present:
		String itemTypeName = configData.getString(itemTypeKey);
		if (itemTypeName != null) {
			int itemData = configData.getInt(itemDataKey);
			// We no longer check for legacy materials:
			Material itemType = DataUtils.loadMaterial(configData, itemTypeKey);
			if (itemType == null) {
				// Fallback to default:
				Log.info("  Migration of '" + itemTypeKey + "' from type '" + itemTypeName
						+ "' and data value '" + itemData
						+ "' is no longer supported. Falling back to default type '"
						+ defaultType.name() + "'.");
				configData.set(itemTypeKey, defaultType.name());
				migrated = true;
			}
		}

		// Remove data value from config:
		if (ConfigMigrationHelper.removeSetting(configData, itemDataKey)) {
			migrated = true;
		}
		return migrated;
	}
}
