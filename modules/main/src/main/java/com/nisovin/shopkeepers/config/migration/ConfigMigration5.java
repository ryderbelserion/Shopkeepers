package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 4 to version 5.
 */
public class ConfigMigration5 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Remove the no longer used zero-currency-item and zero-high-currency-item settings:
		removeSetting(configData, "zero-currency-item");
		removeSetting(configData, "zero-high-currency-item");
	}
}
