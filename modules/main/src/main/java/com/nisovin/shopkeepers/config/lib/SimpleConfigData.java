package com.nisovin.shopkeepers.config.lib;

import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DelegateDataContainer;

/**
 * A basic {@link ConfigData} implementation.
 */
public class SimpleConfigData extends DelegateDataContainer implements ConfigData {

	private DataContainer defaults = null;

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
	public DataContainer getDefaults() {
		return defaults;
	}

	@Override
	public void setDefaults(DataContainer defaults) {
		this.defaults = defaults;
	}
}
