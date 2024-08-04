package com.nisovin.shopkeepers.config.migration;

import java.util.List;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.DataUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Migrates the config from version 1 to version 2.
 */
public class ConfigMigration2 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Convert item data settings to ItemData:
		// Due to the compact representation of ItemData, this is only required for items which
		// previously supported further data (such as custom display name and/or lore).

		// shop-creation-item:
		migrateItem(
				configData,
				"shop-creation-item",
				"shop-creation-item",
				"shop-creation-item-name",
				"shop-creation-item-lore"
		);
		// name-item:
		migrateItem(
				configData,
				"name-item",
				"name-item",
				null,
				"name-item-lore"
		);
		// hire-item:
		migrateItem(
				configData,
				"hire-item",
				"hire-item",
				"hire-item-name",
				"hire-item-lore"
		);
		// currency-item:
		migrateItem(
				configData,
				"currency-item",
				"currency-item",
				"currency-item-name",
				"currency-item-lore"
		);
		// zero-currency-item:
		migrateItem(
				configData,
				"zero-currency-item",
				"zero-currency-item",
				"zero-currency-item-name",
				"zero-currency-item-lore"
		);
		// high-currency-item
		migrateItem(
				configData,
				"high-currency-item",
				"high-currency-item",
				"high-currency-item-name",
				"high-currency-item-lore"
		);
		// high-zero-currency-item -> zero-high-currency-item:
		migrateItem(
				configData,
				"zero-high-currency-item",
				"high-zero-currency-item",
				"high-zero-currency-item-name",
				"high-zero-currency-item-lore"
		);
	}

	// displayNameKey and loreKey can be null if they don't exist
	private static void migrateItem(
			DataContainer configData,
			String newItemKey,
			String itemTypeKey,
			@Nullable String displayNameKey,
			@Nullable String loreKey
	) {
		assert configData != null && newItemKey != null && itemTypeKey != null;
		StringBuilder msgBuilder = new StringBuilder();
		msgBuilder.append("  Migrating item data for '")
				.append(itemTypeKey)
				.append("' (")
				.append(configData.get(itemTypeKey))
				.append(")");
		if (displayNameKey != null) {
			msgBuilder.append(" and '")
					.append(displayNameKey)
					.append("' (")
					.append(configData.get(displayNameKey))
					.append(")");
		}
		if (loreKey != null) {
			msgBuilder.append(" and '")
					.append(loreKey)
					.append("' (")
					.append(configData.get(loreKey))
					.append(")");
		}
		msgBuilder.append(" to new format at '")
				.append(newItemKey)
				.append("'.");
		Log.info(msgBuilder.toString());

		// Item type:
		Material itemType = DataUtils.loadMaterial(configData, itemTypeKey);
		if (itemType == null) {
			Log.warning("    Skipping migration for item '" + itemTypeKey + "'! Unknown material: "
					+ configData.get(itemTypeKey));
			return;
		}

		// Display name:
		String displayName = null;
		if (displayNameKey != null) {
			displayName = configData.getString(displayNameKey);
			if (displayName == null || displayName.isEmpty()) {
				displayName = null; // Normalize empty display name to null
			} else {
				displayName = TextUtils.colorize(displayName);
			}
		}
		// Lore:
		List<String> lore = null;
		if (loreKey != null) {
			lore = ConversionUtils.toStringList(configData.getList(loreKey));
			if (lore == null || lore.isEmpty()) {
				lore = null; // Normalize empty lore to null
			} else {
				lore = TextUtils.colorize(lore);
			}
		}

		// Create ItemData:
		ItemData itemData = new ItemData(itemType, displayName, lore);

		// Remove old data:
		// If old and new key are the same, try to persist the position of the setting inside the
		// config.
		if (!itemTypeKey.equals(newItemKey)) {
			configData.remove(itemTypeKey);
		}
		if (displayNameKey != null) {
			configData.remove(displayNameKey);
		}
		if (loreKey != null) {
			configData.remove(loreKey);
		}

		// Save new data (under potentially new key):
		configData.set(newItemKey, itemData.serialize());
	}
}
