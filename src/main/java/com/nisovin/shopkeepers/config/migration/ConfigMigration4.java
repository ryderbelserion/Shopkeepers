package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.util.Log;

/**
 * Migrates the config from version 3 to version 4.
 */
public class ConfigMigration4 implements ConfigMigration {

	@Override
	public void apply(Configuration config) {
		// Migrate language 'en' to 'en-default':
		migrateValue(config, "language", "en", "en-default");

		// Migrate max shops limit from 0 to -1 (indicating no limit by default):
		migrateValue(config, "max-shops-per-player", 0, -1);
	}

	private static void migrateValue(Configuration config, String key, Object expectedOldValue, Object newValue) {
		if (config.isSet(key)) {
			Object oldValue = config.get(key);
			if (expectedOldValue.equals(oldValue)) {
				Log.info("  Migrating setting '" + key + "' from value '" + oldValue + "' to new value '" + newValue + "'.");
				config.set(key, newValue);
			}
		}
	}
}
