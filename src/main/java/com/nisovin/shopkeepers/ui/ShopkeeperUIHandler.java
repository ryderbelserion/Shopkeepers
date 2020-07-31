package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Validate;

/**
 * An {@link UIHandler} which handles one specific type of user interface for one specific {@link Shopkeeper}.
 */
public abstract class ShopkeeperUIHandler extends UIHandler {

	private final AbstractShopkeeper shopkeeper; // Not null

	protected ShopkeeperUIHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the shopkeeper.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
