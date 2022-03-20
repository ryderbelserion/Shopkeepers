package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

public interface ConfigMigration {

	/**
	 * Applies the config migration.
	 * <p>
	 * If an issue prevents the migration of individual config entries, it is usually preferred to
	 * log a warning and continue the migration, instead of aborting the migration or throwing an
	 * exception.
	 * 
	 * @param configData
	 *            the current config data
	 */
	public void apply(DataContainer configData);
}
