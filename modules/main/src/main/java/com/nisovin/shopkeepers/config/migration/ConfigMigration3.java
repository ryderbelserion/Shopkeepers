package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 2 to version 3.
 */
public class ConfigMigration3 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Convert any settings with 'chest' in their name to new settings with 'container' in their
		// name:
		migrateSetting(
				configData,
				"require-chest-recently-placed",
				"require-container-recently-placed"
		);
		migrateSetting(configData, "max-chest-distance", "max-container-distance");
		migrateSetting(configData, "protect-chests", "protect-containers");
		migrateSetting(
				configData,
				"delete-shopkeeper-on-break-chest",
				"delete-shopkeeper-on-break-container"
		);
		migrateSetting(
				configData,
				"enable-chest-option-on-player-shop",
				"enable-container-option-on-player-shop"
		);
		migrateSetting(configData, "chest-item", "container-item");
		// Note: Most of the following message also had changes to their contents which need to be
		// applied manually.
		// We migrate them here anyway, so that any old message settings get removed from the config
		// and thereby don't cause confusion by there being many no longer used message settings.
		// Note that this migration is not invoked for custom / separate language files.
		migrateSetting(configData, "msg-button-chest", "msg-button-container");
		migrateSetting(configData, "msg-button-chest-lore", "msg-button-container-lore");
		migrateSetting(configData, "msg-selected-chest", "msg-container-selected");
		migrateSetting(configData, "msg-must-select-chest", "msg-must-select-container");
		migrateSetting(configData, "msg-no-chest-selected", "msg-invalid-container");
		migrateSetting(configData, "msg-chest-too-far", "msg-container-too-far-away");
		migrateSetting(configData, "msg-chest-not-placed", "msg-container-not-placed");
		migrateSetting(configData, "msg-chest-already-in-use", "msg-container-already-in-use");
		migrateSetting(configData, "msg-no-chest-access", "msg-no-container-access");
		migrateSetting(configData, "msg-unused-chest", "msg-unused-container");
		migrateSetting(
				configData,
				"msg-cant-trade-with-shop-missing-chest",
				"msg-cant-trade-with-shop-missing-container"
		);
	}
}
