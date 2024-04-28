package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 5 to version 6.
 */
public class ConfigMigration6 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Disable the new 'add-shop-creation-item-tag' and 'identify-shop-creation-item-by-tag'
		// settings by default for existing servers:
		addSetting(configData, "add-shop-creation-item-tag", false);
		addSetting(configData, "identify-shop-creation-item-by-tag", false);
	}
}
