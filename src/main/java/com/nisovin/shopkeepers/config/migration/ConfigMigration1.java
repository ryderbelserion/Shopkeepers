package com.nisovin.shopkeepers.config.migration;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;

/**
 * Migrate the config from version &lt;= 0 to version 1.
 */
public class ConfigMigration1 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// pre 1.13 to 1.13:
		// migrate shop creation item, if present:
		String shopCreationItemTypeName = config.getString("shop-creation-item", null);
		if (shopCreationItemTypeName != null) {
			// note: this takes defaults into account:
			Material shopCreationItem = ConfigUtils.loadMaterial(config, "shop-creation-item", true);
			String shopCreationItemSpawnEggEntityType = config.getString("shop-creation-item-spawn-egg-entity-type");
			if (shopCreationItem == Material.LEGACY_MONSTER_EGG && !StringUtils.isEmpty(shopCreationItemSpawnEggEntityType)) {
				// migrate spawn egg (ignores the data value): spawn eggs are different materials now
				EntityType spawnEggEntityType = null;
				try {
					spawnEggEntityType = EntityType.valueOf(shopCreationItemSpawnEggEntityType);
				} catch (IllegalArgumentException e) {
					// unknown entity type
				}
				Material newShopCreationItem = LegacyConversion.fromLegacySpawnEgg(spawnEggEntityType);

				boolean usingDefault = false;
				if (newShopCreationItem == null || newShopCreationItem == Material.AIR) {
					// fallback to default:
					newShopCreationItem = Material.VILLAGER_SPAWN_EGG;
					usingDefault = true;
				}
				assert newShopCreationItem != null;

				Log.info("  Migrating 'shop-creation-item' from '" + shopCreationItemTypeName + "' and spawn egg entity type '"
						+ shopCreationItemSpawnEggEntityType + "' to '" + newShopCreationItem + "'" + (usingDefault ? " (default)" : "") + ".");
				config.set("shop-creation-item", newShopCreationItem.name());
			} else {
				// regular material + data value migration:
				migrateLegacyItemData(config, "shop-creation-item", "shop-creation-item", "shop-creation-item-data", Material.VILLAGER_SPAWN_EGG);
			}
		}

		// remove shop-creation-item-spawn-egg-entity-type from config:
		if (config.isSet("shop-creation-item-spawn-egg-entity-type")) {
			Log.info("  Removing 'shop-creation-item-spawn-egg-entity-type' (previously '" + config.get("shop-creation-item-spawn-egg-entity-type", null) + "').");
			config.set("shop-creation-item-spawn-egg-entity-type", null);
		}

		// remove shop-creation-item-data-value from config:
		if (config.isSet("shop-creation-item-data")) {
			Log.info("  Removing 'shop-creation-item-data' (previously '" + config.get("shop-creation-item-data", null) + "').");
			config.set("shop-creation-item-data", null);
		}

		// name item:
		migrateLegacyItemData(config, "name-item", "name-item", "name-item-data", Material.NAME_TAG);

		// chest item:
		migrateLegacyItemData(config, "chest-item", "chest-item", "chest-item-data", Material.CHEST);

		// delete item:
		migrateLegacyItemData(config, "delete-item", "delete-item", "delete-item-data", Material.BONE);

		// hire item:
		migrateLegacyItemData(config, "hire-item", "hire-item", "hire-item-data", Material.EMERALD);

		// currency item:
		migrateLegacyItemData(config, "currency-item", "currency-item", "currency-item-data", Material.EMERALD);

		// zero currency item:
		migrateLegacyItemData(config, "zero-currency-item", "zero-currency-item", "zero-currency-item-data", Material.BARRIER);

		// high currency item:
		migrateLegacyItemData(config, "high-currency-item", "high-currency-item", "high-currency-item-data", Material.EMERALD_BLOCK);

		// high zero currency item:
		migrateLegacyItemData(config, "high-zero-currency-item", "high-zero-currency-item", "high-zero-currency-item-data", Material.BARRIER);
	}

	// convert legacy material + data value to new material, returns true if migrations took place
	private static boolean migrateLegacyItemData(ConfigurationSection config, String migratedItemId, String itemTypeKey, String itemDataKey, Material defaultType) {
		boolean migrated = false;

		// migrate material, if present:
		String itemTypeName = config.getString(itemTypeKey, null);
		if (itemTypeName != null) {
			Material newItemType = null;
			int itemData = config.getInt(itemDataKey, 0);
			Material itemType = ConfigUtils.loadMaterial(config, itemTypeKey, true);
			if (itemType != null) {
				newItemType = LegacyConversion.fromLegacy(itemType, (byte) itemData);
			}
			boolean usingDefault = false;
			if (newItemType == null || newItemType == Material.AIR) {
				// fallback to default:
				newItemType = defaultType;
				usingDefault = true;
			}
			if (itemType != newItemType) {
				Log.info("  Migrating '" + migratedItemId + "' from type '" + itemTypeName + "' and data value '" + itemData + "' to type '"
						+ (newItemType == null ? "" : newItemType.name()) + "'" + (usingDefault ? " (default)" : "") + ".");
				config.set(itemTypeKey, (newItemType != null ? newItemType.name() : null));
				migrated = true;
			}
		}

		// remove data value from config:
		if (config.isSet(itemDataKey)) {
			Log.info("  Removing '" + itemDataKey + "' (previously '" + config.get(itemDataKey, null) + "').");
			config.set(itemDataKey, null);
			migrated = true;
		}
		return migrated;
	}
}
