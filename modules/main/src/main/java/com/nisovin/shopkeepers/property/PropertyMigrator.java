package com.nisovin.shopkeepers.property;

import com.nisovin.shopkeepers.util.data.DataContainer;

/**
 * Applies data migrations before a {@link Property} {@link Property#load(DataContainer) loads} its value.
 * <p>
 * The migrator has access the complete {@link DataContainer}, and not just the currently stored value for the
 * property's {@link Property#getKey() key}, because migrations might need to move data from a previously used key to
 * the currently used key.
 */
@FunctionalInterface
public interface PropertyMigrator {

	/**
	 * Applies data migrations to the given {@link DataContainer}.
	 * 
	 * @param property
	 *            the involved property, not <code>null</code>
	 * @param dataContainer
	 *            the data container, not <code>null</code>
	 */
	public void migrate(Property<?> property, DataContainer dataContainer);
}
