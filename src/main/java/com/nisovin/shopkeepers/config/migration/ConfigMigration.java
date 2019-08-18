package com.nisovin.shopkeepers.config.migration;

import org.bukkit.configuration.Configuration;

public interface ConfigMigration {

	public void apply(Configuration config);
}
