package com.nisovin.shopkeepers.property;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link PropertyContainer} that is associated with a specific {@link Shopkeeper}.
 */
public class ShopkeeperPropertyContainer extends AbstractPropertyContainer {

	private final AbstractShopkeeper shopkeeper;

	/**
	 * Creates a new {@link ShopkeeperPropertyContainer}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 */
	public ShopkeeperPropertyContainer(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the {@link Shopkeeper} this property container is associated with.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	public String getLogPrefix() {
		return shopkeeper.getLogPrefix();
	}

	@Override
	public void markDirty() {
		shopkeeper.markDirty();
	}
}
