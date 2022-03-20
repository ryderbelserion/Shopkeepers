package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 3 to version 4.
 */
public class ConfigMigration4 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Migrate language 'en' to 'en-default':
		migrateValue(configData, "language", "en", "en-default");

		// Migrate max shops limit from 0 to -1 (indicating no limit by default):
		migrateValue(configData, "max-shops-per-player", 0, -1);

		// Setting 'enable-spawn-verifier' got removed:
		removeSetting(configData, "enable-spawn-verifier");

		migrateSetting(configData, "enable-purchase-logging", "log-trades-to-csv");
	}
}
