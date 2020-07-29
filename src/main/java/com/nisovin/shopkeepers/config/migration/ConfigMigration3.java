package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.util.Log;

/**
 * Migrates the config from version 2 to version 3.
 */
public class ConfigMigration3 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// Convert any settings with 'chest' in their name to new settings with 'container' in their name:
		migrateSetting(config, "require-chest-recently-placed", "require-container-recently-placed");
		migrateSetting(config, "max-chest-distance", "max-container-distance");
		migrateSetting(config, "protect-chests", "protect-containers");
		migrateSetting(config, "delete-shopkeeper-on-break-chest", "delete-shopkeeper-on-break-container");
		migrateSetting(config, "enable-chest-option-on-player-shop", "enable-container-option-on-player-shop");
		migrateSetting(config, "chest-item", "container-item");
		// Note: Most of the following message also had changes to their contents which need to be applied manually.
		// We migrate them here anyways, so that any old message settings get removed from the config and thereby don't
		// cause confusion by there being many no longer used message settings. Note that this migration is not invoked
		// for custom / separate language files.
		migrateSetting(config, "msg-button-chest", "msg-button-container");
		migrateSetting(config, "msg-button-chest-lore", "msg-button-container-lore");
		migrateSetting(config, "msg-selected-chest", "msg-container-selected");
		migrateSetting(config, "msg-must-select-chest", "msg-must-select-container");
		migrateSetting(config, "msg-no-chest-selected", "msg-invalid-container");
		migrateSetting(config, "msg-chest-too-far", "msg-container-too-far-away");
		migrateSetting(config, "msg-chest-not-placed", "msg-container-not-placed");
		migrateSetting(config, "msg-chest-already-in-use", "msg-container-already-in-use");
		migrateSetting(config, "msg-no-chest-access", "msg-no-container-access");
		migrateSetting(config, "msg-unused-chest", "msg-unused-container");
		migrateSetting(config, "msg-cant-trade-with-shop-missing-chest", "msg-cant-trade-with-shop-missing-container");
	}

	private static void migrateSetting(Configuration config, String oldKey, String newKey) {
		assert config != null && oldKey != null && newKey != null;
		if (config.isSet(oldKey) && !config.isSet(newKey)) {
			Object oldValue = config.get(oldKey);
			Log.info("  Migrating setting '" + oldKey + "' to '" + newKey + "'. Value: " + oldValue);
			config.set(newKey, oldValue);
			config.set(oldKey, null);
		}
	}
}
