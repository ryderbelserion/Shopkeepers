package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 2 to version 3.
 */
public class ConfigMigration3 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Convert any settings with 'chest' in their name to new settings with 'container' in their name:
		ConfigMigrationHelper.migrateSetting(configData, "require-chest-recently-placed", "require-container-recently-placed");
		ConfigMigrationHelper.migrateSetting(configData, "max-chest-distance", "max-container-distance");
		ConfigMigrationHelper.migrateSetting(configData, "protect-chests", "protect-containers");
		ConfigMigrationHelper.migrateSetting(configData, "delete-shopkeeper-on-break-chest", "delete-shopkeeper-on-break-container");
		ConfigMigrationHelper.migrateSetting(configData, "enable-chest-option-on-player-shop", "enable-container-option-on-player-shop");
		ConfigMigrationHelper.migrateSetting(configData, "chest-item", "container-item");
		// Note: Most of the following message also had changes to their contents which need to be applied manually.
		// We migrate them here anyways, so that any old message settings get removed from the config and thereby don't
		// cause confusion by there being many no longer used message settings. Note that this migration is not invoked
		// for custom / separate language files.
		ConfigMigrationHelper.migrateSetting(configData, "msg-button-chest", "msg-button-container");
		ConfigMigrationHelper.migrateSetting(configData, "msg-button-chest-lore", "msg-button-container-lore");
		ConfigMigrationHelper.migrateSetting(configData, "msg-selected-chest", "msg-container-selected");
		ConfigMigrationHelper.migrateSetting(configData, "msg-must-select-chest", "msg-must-select-container");
		ConfigMigrationHelper.migrateSetting(configData, "msg-no-chest-selected", "msg-invalid-container");
		ConfigMigrationHelper.migrateSetting(configData, "msg-chest-too-far", "msg-container-too-far-away");
		ConfigMigrationHelper.migrateSetting(configData, "msg-chest-not-placed", "msg-container-not-placed");
		ConfigMigrationHelper.migrateSetting(configData, "msg-chest-already-in-use", "msg-container-already-in-use");
		ConfigMigrationHelper.migrateSetting(configData, "msg-no-chest-access", "msg-no-container-access");
		ConfigMigrationHelper.migrateSetting(configData, "msg-unused-chest", "msg-unused-container");
		ConfigMigrationHelper.migrateSetting(configData, "msg-cant-trade-with-shop-missing-chest", "msg-cant-trade-with-shop-missing-container");
	}
}
