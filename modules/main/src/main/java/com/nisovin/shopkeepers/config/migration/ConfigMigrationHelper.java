package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class ConfigMigrationHelper {

	// Returns true if the setting was newly added.
	public static boolean addSetting(DataContainer configData, String key, Object value) {
		Validate.notNull(configData, "configData is null");
		Validate.notNull(key, "key is null");
		if (!configData.contains(key)) {
			Log.info("  Adding setting '" + key + "' with value: " + value);
			configData.set(key, value);
			return true;
		}
		return false;
	}

	// Returns true if there was a value set that got removed.
	public static boolean removeSetting(DataContainer configData, String key) {
		Validate.notNull(configData, "configData is null");
		Validate.notNull(key, "key is null");
		if (configData.contains(key)) {
			Log.info("  Removing setting '" + key + "'. Previous value: " + configData.get(key));
			configData.remove(key);
			return true;
		}
		return false;
	}

	// Returns true if there has been a matching value that got replaced.
	public static boolean migrateValue(
			DataContainer configData,
			String key,
			Object expectedOldValue,
			Object newValue
	) {
		Validate.notNull(configData, "configData is null");
		Validate.notNull(key, "key is null");
		Validate.notNull(expectedOldValue, "expectedOldValue is null");
		if (configData.contains(key)) {
			Object oldValue = configData.get(key);
			if (expectedOldValue.equals(oldValue)) {
				Log.info("  Migrating setting '" + key + "' from value '" + oldValue
						+ "' to new value '" + newValue + "'.");
				configData.set(key, newValue);
				return true;
			}
		}
		return false;
	}

	// Returns true if the setting got migrated.
	public static boolean migrateSetting(DataContainer configData, String oldKey, String newKey) {
		Validate.notNull(configData, "configData is null");
		Validate.notNull(oldKey, "oldKey is null");
		Validate.notNull(newKey, "newKey is null");
		if (configData.contains(oldKey) && !configData.contains(newKey)) {
			Object oldValue = configData.get(oldKey);
			Log.info("  Migrating setting '" + oldKey + "' to '" + newKey
					+ "'. Value: " + oldValue);
			configData.set(newKey, oldValue);
			configData.remove(oldKey);
			return true;
		}
		return false;
	}

	private ConfigMigrationHelper() {
	}
}
