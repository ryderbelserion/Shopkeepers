package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 6 to version 7.
 */
public class ConfigMigration7 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Remove the file-encoding setting: We always save and load as UTF-8.
		removeSetting(configData, "file-encoding");
	}
}
