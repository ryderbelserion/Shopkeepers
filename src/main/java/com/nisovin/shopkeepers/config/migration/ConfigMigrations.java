package com.nisovin.shopkeepers.config.migration;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.Configuration;

import com.nisovin.shopkeepers.util.Log;

public class ConfigMigrations {

	private static final String CONFIG_VERSION_KEY = "config-version";
	// each index corresponds to a source config version and its migration to the next version
	private static final List<ConfigMigration> migrations = Arrays.asList(new ConfigMigration1(), new ConfigMigration2());

	// returns true if any migrations got applied (if the config has potentially been modified)
	public static boolean applyMigrations(Configuration config) {
		// no migrations are required if the config is empty (missing entries will get generated from defaults):
		if (config.getKeys(false).isEmpty()) return false;

		boolean migrated = false;
		int configVersion = config.getInt(CONFIG_VERSION_KEY, 0); // default value is important here
		if (configVersion < 0) {
			configVersion = 0; // first version is 0
		}
		for (int version = configVersion; version < migrations.size(); ++version) {
			int nextVersion = (version + 1);
			ConfigMigration migration = migrations.get(version);
			Log.info("Migrating config from version " + version + " to version " + nextVersion + " ..");
			if (migration != null) {
				migration.apply(config);
			}
			// update config version:
			config.set(CONFIG_VERSION_KEY, nextVersion);
			migrated = true;
			Log.info("Config migrated to version " + nextVersion + "." + (migration == null ? " (skipped)" : ""));
		}
		return migrated;
	}

	private ConfigMigrations() {
	}
}
