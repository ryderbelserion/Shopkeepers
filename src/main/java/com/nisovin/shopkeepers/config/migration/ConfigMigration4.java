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
		String key = "language";
		if (config.isSet(key)) {
			Object oldValue = config.get(key);
			if (oldValue.equals("en")) {
				Log.info("  Migrating setting '" + key + "' from value 'en' to new value 'en-default'.");
				config.set(key, "en-default");
			}
		}
	}
}
