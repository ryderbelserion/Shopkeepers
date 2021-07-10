package com.nisovin.shopkeepers.api.shopkeeper.admin;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * A shopkeeper that is managed by the server.
 */
public interface AdminShopkeeper extends Shopkeeper {

	/**
	 * Gets the permission that is required to trade with this {@link AdminShopkeeper}.
	 * 
	 * @return the permission, or <code>null</code> if no permission is required
	 */
	public String getTradePermission();

	/**
	 * Sets the permission that is required to trade with this {@link AdminShopkeeper}.
	 * 
	 * @param tradePermission
	 *            the required permission, or <code>null</code> if no permission is required
	 */
	public void setTradePermission(String tradePermission);
}
