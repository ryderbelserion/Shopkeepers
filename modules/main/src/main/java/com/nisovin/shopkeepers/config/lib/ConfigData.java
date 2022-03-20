package com.nisovin.shopkeepers.config.lib;

import org.bukkit.configuration.Configuration;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.bukkit.BukkitConfigData;
import com.nisovin.shopkeepers.util.data.container.ConfigBasedDataContainer;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataContainer} that stores configuration data and provides additional configuration
 * specific functionality, such as access to default values.
 */
public interface ConfigData extends DataContainer {

	/**
	 * Creates a new empty {@link ConfigData} instance.
	 * <p>
	 * This factory method has the same effect as {@link #of(DataContainer) creating a new
	 * ConfigData instance} based on a {@link DataContainer#create() newly created empty
	 * DataContainer}.
	 * 
	 * @return the new config data
	 */
	public static ConfigData create() {
		return of(DataContainer.create());
	}

	/**
	 * Creates a new {@link ConfigData} instance that uses the given {@link DataContainer} to store
	 * its config values.
	 * <p>
	 * If the given data container is a {@link ConfigBasedDataContainer}, this returns a
	 * {@link ConfigData} implementation that can retrieve default values from the underlying Bukkit
	 * {@link Configuration}.
	 * 
	 * @param dataContainer
	 *            the underlying data container that stores the config data, not <code>null</code>
	 * @return the config data, not <code>null</code>
	 */
	public static ConfigData of(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		if (dataContainer instanceof ConfigBasedDataContainer) {
			return new BukkitConfigData((ConfigBasedDataContainer) dataContainer);
		} else {
			return new SimpleConfigData(dataContainer);
		}
	}

	/**
	 * Creates a new {@link ConfigData} instance for the given data source.
	 * <p>
	 * This factory method has the same effect as {@link #of(DataContainer) creating a new
	 * ConfigData} based on the {@link DataContainer#of(Object) DataContainer created for the given
	 * data source}.
	 * 
	 * @param dataSource
	 *            the data source, not <code>null</code>
	 * @return the config data, not <code>null</code>
	 */
	public static ConfigData of(Object dataSource) {
		return of(DataContainer.ofNonNull(dataSource));
	}

	/////

	/**
	 * Gets the default values for this configuration data.
	 * 
	 * @return the default values, or <code>null</code> if there are no default values
	 */
	public @Nullable DataContainer getDefaults();

	/**
	 * Sets the default values for this configuration data.
	 * 
	 * @param defaults
	 *            the default values, can be <code>null</code> to unset any previously set default
	 *            values
	 */
	public void setDefaults(@Nullable DataContainer defaults);
}
