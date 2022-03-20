package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

/**
 * Interface for {@link UIHandler UIHandlers} which handle one specific type of user interface for
 * one specific {@link Shopkeeper}.
 */
public interface ShopkeeperUIHandler {

	/**
	 * Gets the shopkeeper.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public AbstractShopkeeper getShopkeeper();
}
