package com.nisovin.shopkeepers.config.lib;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.DelegateDataContainer;

/**
 * A basic {@link ConfigData} implementation.
 */
public class SimpleConfigData extends DelegateDataContainer implements ConfigData {

	private @Nullable DataContainer defaults = null;

	/**
	 * Creates a new {@link SimpleConfigData}.
	 * 
	 * @param dataContainer
	 *            the underlying data container that stores the config values, not <code>null</code>
	 */
	public SimpleConfigData(DataContainer dataContainer) {
		super(dataContainer);
	}

	@Override
	public @Nullable DataContainer getDefaults() {
		return defaults;
	}

	@Override
	public void setDefaults(@Nullable DataContainer defaults) {
		this.defaults = defaults;
	}
}
