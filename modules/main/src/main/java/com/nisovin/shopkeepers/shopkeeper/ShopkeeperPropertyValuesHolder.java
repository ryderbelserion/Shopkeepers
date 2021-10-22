package com.nisovin.shopkeepers.shopkeeper;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.util.data.property.value.AbstractPropertyValuesHolder;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValuesHolder;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link PropertyValuesHolder} that is associated with a specific {@link Shopkeeper}.
 */
public class ShopkeeperPropertyValuesHolder extends AbstractPropertyValuesHolder {

	private final AbstractShopkeeper shopkeeper;

	/**
	 * Creates a new {@link ShopkeeperPropertyValuesHolder}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 */
	public ShopkeeperPropertyValuesHolder(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the {@link Shopkeeper} this {@link PropertyValuesHolder} is associated with.
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
