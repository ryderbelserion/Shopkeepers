package com.nisovin.shopkeepers.property;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Applies data migrations before a {@link Property} {@link Property#load(ConfigurationSection) loads} its value.
 */
@FunctionalInterface
public interface PropertyMigrator {

	/**
	 * Applies data migrations to the given {@link ConfigurationSection}.
	 * 
	 * @param property
	 *            the involved property, not <code>null</code>
	 * @param configSection
	 *            the configuration section, not <code>null</code>
	 */
	public void migrate(Property<?> property, ConfigurationSection configSection);
}
