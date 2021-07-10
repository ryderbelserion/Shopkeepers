package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.Configuration;

/**
 * Migrates the config from version 2 to version 3.
 */
public class ConfigMigration3 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// Convert any settings with 'chest' in their name to new settings with 'container' in their name:
		ConfigMigrationHelper.migrateSetting(config, "require-chest-recently-placed", "require-container-recently-placed");
		ConfigMigrationHelper.migrateSetting(config, "max-chest-distance", "max-container-distance");
		ConfigMigrationHelper.migrateSetting(config, "protect-chests", "protect-containers");
		ConfigMigrationHelper.migrateSetting(config, "delete-shopkeeper-on-break-chest", "delete-shopkeeper-on-break-container");
		ConfigMigrationHelper.migrateSetting(config, "enable-chest-option-on-player-shop", "enable-container-option-on-player-shop");
		ConfigMigrationHelper.migrateSetting(config, "chest-item", "container-item");
		// Note: Most of the following message also had changes to their contents which need to be applied manually.
		// We migrate them here anyways, so that any old message settings get removed from the config and thereby don't
		// cause confusion by there being many no longer used message settings. Note that this migration is not invoked
		// for custom / separate language files.
		ConfigMigrationHelper.migrateSetting(config, "msg-button-chest", "msg-button-container");
		ConfigMigrationHelper.migrateSetting(config, "msg-button-chest-lore", "msg-button-container-lore");
		ConfigMigrationHelper.migrateSetting(config, "msg-selected-chest", "msg-container-selected");
		ConfigMigrationHelper.migrateSetting(config, "msg-must-select-chest", "msg-must-select-container");
		ConfigMigrationHelper.migrateSetting(config, "msg-no-chest-selected", "msg-invalid-container");
		ConfigMigrationHelper.migrateSetting(config, "msg-chest-too-far", "msg-container-too-far-away");
		ConfigMigrationHelper.migrateSetting(config, "msg-chest-not-placed", "msg-container-not-placed");
		ConfigMigrationHelper.migrateSetting(config, "msg-chest-already-in-use", "msg-container-already-in-use");
		ConfigMigrationHelper.migrateSetting(config, "msg-no-chest-access", "msg-no-container-access");
		ConfigMigrationHelper.migrateSetting(config, "msg-unused-chest", "msg-unused-container");
		ConfigMigrationHelper.migrateSetting(config, "msg-cant-trade-with-shop-missing-chest", "msg-cant-trade-with-shop-missing-container");
	}
}
