package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.Configuration;

/**
 * Migrates the config from version 3 to version 4.
 */
public class ConfigMigration4 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// Migrate language 'en' to 'en-default':
		ConfigMigrationHelper.migrateValue(config, "language", "en", "en-default");

		// Migrate max shops limit from 0 to -1 (indicating no limit by default):
		ConfigMigrationHelper.migrateValue(config, "max-shops-per-player", 0, -1);

		// Setting 'enable-spawn-verifier' got removed:
		ConfigMigrationHelper.removeSetting(config, "enable-spawn-verifier");

		ConfigMigrationHelper.migrateSetting(config, "enable-purchase-logging", "log-trades-to-csv");
	}
}
