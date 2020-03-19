package com.nisovin.shopkeepers.api.ui;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

public interface UISession {

	public Shopkeeper getShopkeeper();

	public UIType getUIType();

	/**
	 * The session becomes invalid once the player closes the inventory view.
	 * 
	 * @return whether the session is still valid
	 */
	public boolean isValid();
}
