package com.nisovin.shopkeepers.config.lib.bukkit;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.ConfigData;
import com.nisovin.shopkeepers.config.lib.SimpleConfigData;
import com.nisovin.shopkeepers.util.data.container.ConfigBasedDataContainer;
import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * A {@link ConfigData} implementation that extracts its default values from an underlying Bukkit
 * {@link Configuration} the first time the defaults are accessed, unless the default values have
 * been explicitly set via {@link #setDefaults(DataContainer)}.
 * <p>
 * The default values for the configuration section wrapped by the underlying
 * {@link ConfigBasedDataContainer} are accessed via
 * {@link ConfigurationSection#getDefaultSection()}. This underlying default configuration section
 * is only retrieved once, the first time the default values are accessed, and only if no default
 * values have been {@link #setDefaults(DataContainer) explicitly set} yet. The corresponding data
 * container reads and writes through to the underlying default configuration section. If this
 * underlying default configuration section is dynamically replaced inside the underlying Bukkit
 * {@link Configuration}, this change might not be reflected by this {@link ConfigData}.
 * <p>
 * Default values set via {@link #setDefaults(DataContainer)} have no effect on the
 * {@link Configuration#getDefaults() default values} that may be set for the underlying Bukkit
 * {@link Configuration}.
 */
public class BukkitConfigData extends SimpleConfigData {

	private boolean defaultsSetup = false;

	/**
	 * Creates a new {@link BukkitConfigData} based on the given {@link ConfigBasedDataContainer}.
	 * 
	 * @param dataContainer
	 *            the underlying config based data container that stores the config values and might
	 *            also provide the default values, not <code>null</code>
	 */
	public BukkitConfigData(ConfigBasedDataContainer dataContainer) {
		super(dataContainer);
	}

	private @Nullable ConfigurationSection getDefaultConfig() {
		// Can be null:
		return ((ConfigBasedDataContainer) dataContainer).getConfig().getDefaultSection();
	}

	@Override
	public @Nullable DataContainer getDefaults() {
		if (!defaultsSetup) {
			this.setDefaults(DataContainer.of(this.getDefaultConfig()));
		}
		return super.getDefaults();
	}

	@Override
	public void setDefaults(@Nullable DataContainer defaults) {
		super.setDefaults(defaults);
		defaultsSetup = true;
	}
}
