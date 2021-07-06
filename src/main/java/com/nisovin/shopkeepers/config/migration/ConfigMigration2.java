package com.nisovin.shopkeepers.config.migration;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Migrates the config from version 1 to version 2.
 */
public class ConfigMigration2 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// Convert item data settings to ItemData:
		// Due to the compact representation of ItemData, this is only required for items which previously supported
		// further data (such as custom display name and/or lore).

		// shop-creation-item:
		migrateItem(config, "shop-creation-item", "shop-creation-item", "shop-creation-item-name", "shop-creation-item-lore");
		// name-item:
		migrateItem(config, "name-item", "name-item", null, "name-item-lore");
		// hire-item:
		migrateItem(config, "hire-item", "hire-item", "hire-item-name", "hire-item-lore");
		// currency-item:
		migrateItem(config, "currency-item", "currency-item", "currency-item-name", "currency-item-lore");
		// zero-currency-item:
		migrateItem(config, "zero-currency-item", "zero-currency-item", "zero-currency-item-name", "zero-currency-item-lore");
		// high-currency-item
		migrateItem(config, "high-currency-item", "high-currency-item", "high-currency-item-name", "high-currency-item-lore");
		// high-zero-currency-item -> zero-high-currency-item:
		migrateItem(config, "zero-high-currency-item", "high-zero-currency-item", "high-zero-currency-item-name", "high-zero-currency-item-lore");
	}

	// displayNameKey and loreKey can be null if they don't exist
	private static void migrateItem(Configuration config, String newItemKey, String itemTypeKey, String displayNameKey, String loreKey) {
		assert config != null && itemTypeKey != null;
		StringBuilder msgBuilder = new StringBuilder();
		msgBuilder.append("  Migrating item data for '")
				.append(itemTypeKey)
				.append("' (")
				.append(config.get(itemTypeKey))
				.append(")");
		if (displayNameKey != null) {
			msgBuilder.append(" and '")
					.append(displayNameKey)
					.append("' (")
					.append(config.get(displayNameKey))
					.append(")");
		}
		if (loreKey != null) {
			msgBuilder.append(" and '")
					.append(loreKey)
					.append("' (")
					.append(config.get(loreKey))
					.append(")");
		}
		msgBuilder.append(" to new format at '" + newItemKey + "'.");
		Log.info(msgBuilder.toString());

		// Item type:
		Material itemType = ConfigUtils.loadMaterial(config, itemTypeKey);
		if (itemType == null) {
			Log.warning("    Skipping migration for item '" + itemTypeKey + "'! Unknown material: " + config.get(itemTypeKey));
			return;
		}

		// Display name:
		String displayName = null;
		if (displayNameKey != null) {
			displayName = TextUtils.colorize(config.getString(displayNameKey));
			if (StringUtils.isEmpty(displayName)) {
				displayName = null; // Normalize empty display name to null
			}
		}
		// lore:
		List<String> lore = null;
		if (loreKey != null) {
			lore = TextUtils.colorize(config.getStringList(loreKey));
			if (lore == null || lore.isEmpty()) {
				lore = null; // Normalize empty lore to null
			}
		}

		// Create ItemData:
		ItemData itemData = new ItemData(itemType, displayName, lore);

		// Remove old data:
		// If old and new key are the same, try to persist the position of the setting inside the config.
		if (!itemTypeKey.equals(newItemKey)) {
			config.set(itemTypeKey, null);
		}
		if (displayNameKey != null) {
			config.set(displayNameKey, null);
		}
		if (loreKey != null) {
			config.set(loreKey, null);
		}

		// Save new data (under potentially new key):
		config.set(newItemKey, itemData.serialize());
	}
}
