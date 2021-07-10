package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.util.logging.Log;

public class ConfigMigrationHelper {

	// Returns true if there was a value set that got removed.
	public static boolean removeSetting(ConfigurationSection config, String key) {
		if (config.isSet(key)) {
			Log.info("  Removing setting '" + key + "'. Previous value: " + config.get(key));
			config.set(key, null);
			return true;
		}
		return false;
	}

	// Returns true if there has been a matching value that got replaced.
	public static boolean migrateValue(ConfigurationSection config, String key, Object expectedOldValue, Object newValue) {
		assert expectedOldValue != null;
		if (config.isSet(key)) {
			Object oldValue = config.get(key);
			if (expectedOldValue.equals(oldValue)) {
				Log.info("  Migrating setting '" + key + "' from value '" + oldValue + "' to new value '" + newValue + "'.");
				config.set(key, newValue);
				return true;
			}
		}
		return false;
	}

	// Returns true if the setting got migrated.
	public static boolean migrateSetting(ConfigurationSection config, String oldKey, String newKey) {
		assert config != null && oldKey != null && newKey != null;
		if (config.isSet(oldKey) && !config.isSet(newKey)) {
			Object oldValue = config.get(oldKey);
			Log.info("  Migrating setting '" + oldKey + "' to '" + newKey + "'. Value: " + oldValue);
			config.set(newKey, oldValue);
			config.set(oldKey, null);
			return true;
		}
		return false;
	}

	private ConfigMigrationHelper() {
	}
}
