package com.nisovin.shopkeepers.config.migration;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.config.ConfigLoadException;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;

public class ConfigMigrations {

	private static final String CONFIG_VERSION_KEY = "config-version";
	private static final int FIRST_VERSION = 0; // Also applies if the config version is missing

	// Each index corresponds to a source config version and its migration to the next version
	private static final List<ConfigMigration> migrations = Arrays.asList(
			new ConfigMigration1(),
			new ConfigMigration2(),
			new ConfigMigration3(),
			new ConfigMigration4()
	);

	public static int getLatestVersion() {
		return migrations.size();
	}

	// Returns true if any migrations got applied (if the config has potentially been modified)
	public static boolean applyMigrations(Configuration config) throws ConfigLoadException {
		// No migrations are required if the config is empty (missing entries will get generated from defaults):
		if (config.getKeys(false).isEmpty()) return false;

		// Parse config version:
		int configVersion;
		Object rawConfigVersion = config.get(CONFIG_VERSION_KEY, null); // Explicit default is important here
		if (rawConfigVersion == null) {
			Log.info("Missing config version. Assuming version '" + FIRST_VERSION + "'.");
			configVersion = FIRST_VERSION;
		} else {
			Integer configVersionI = ConversionUtils.toInteger(rawConfigVersion);
			if (configVersionI == null) { // Parsing failed
				throw new ConfigLoadException("Could not parse config version: " + rawConfigVersion);
			} else {
				configVersion = configVersionI.intValue();
			}
		}

		// Check bounds:
		if (configVersion < FIRST_VERSION) {
			throw new ConfigLoadException("Invalid config version: " + rawConfigVersion);
		}
		if (configVersion > getLatestVersion()) {
			throw new ConfigLoadException("Invalid config version: " + configVersion + " (the latest version is " + getLatestVersion() + ")");
		}

		// Apply required migrations:
		boolean migrated = false;
		for (int version = configVersion; version < migrations.size(); ++version) {
			int nextVersion = (version + 1);
			ConfigMigration migration = migrations.get(version);
			Log.info("Migrating config from version " + version + " to version " + nextVersion + " ..");
			if (migration != null) {
				try {
					migration.apply(config);
				} catch (Exception e) {
					throw new ConfigLoadException("Config migration failed with an error!", e);
				}
			}
			// Update config version:
			config.set(CONFIG_VERSION_KEY, nextVersion);
			migrated = true;
			Log.info("Config migrated to version " + nextVersion + "." + (migration == null ? " (no changes)" : ""));
		}
		return migrated;
	}

	private ConfigMigrations() {
	}
}
