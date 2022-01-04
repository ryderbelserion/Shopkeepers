package com.nisovin.shopkeepers.shopkeeper;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.component.ComponentHolder;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link ComponentHolder} that is associated with a particular {@link Shopkeeper}.
 */
public class ShopkeeperComponentHolder extends ComponentHolder {

	private final AbstractShopkeeper shopkeeper;

	public ShopkeeperComponentHolder(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the {@link Shopkeeper} this {@link ComponentHolder} is associated with.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
