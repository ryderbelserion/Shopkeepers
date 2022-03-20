package com.nisovin.shopkeepers.api.internal;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * Extended {@link ShopkeepersPlugin} interface that provides access to additional internal
 * (non-API) components.
 */
public interface InternalShopkeepersPlugin extends ShopkeepersPlugin {

	/**
	 * Gets the {@link ApiInternals}.
	 * 
	 * @return the internals, not <code>null</code>
	 */
	public ApiInternals getApiInternals();
}
