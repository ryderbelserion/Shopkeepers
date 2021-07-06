package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Basic implementation of {@link ShopkeeperUIHandler}.
 */
public abstract class AbstractShopkeeperUIHandler extends UIHandler implements ShopkeeperUIHandler {

	private final AbstractShopkeeper shopkeeper; // Not null

	protected AbstractShopkeeperUIHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
