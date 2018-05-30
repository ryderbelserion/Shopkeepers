package com.nisovin.shopkeepers.shoptypes;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;

public abstract class PlayerShopType<T extends PlayerShopkeeper> extends ShopType<T> {

	protected PlayerShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	// common functions that might be useful for sub-classes:

	@Override
	protected void validateCreationData(ShopCreationData creationData) throws ShopkeeperCreateException {
		super.validateCreationData(creationData);
		if (!(creationData instanceof PlayerShopCreationData)) {
			throw new ShopkeeperCreateException("Expecting player shop creation data!");
		}
	}
}
