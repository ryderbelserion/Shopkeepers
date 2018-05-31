package com.nisovin.shopkeepers.shoptypes;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.AbstractShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopType;

public abstract class AbstractPlayerShopType<T extends PlayerShopkeeper> extends AbstractShopType<T> implements PlayerShopType<T> {

	protected AbstractPlayerShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	// common functions that might be useful for sub-classes:

	@Override
	protected void validateCreationData(ShopCreationData creationData) throws ShopkeeperCreateException {
		super.validateCreationData(creationData);
		Validate.isTrue(creationData instanceof PlayerShopCreationData,
				"Expecting PlayerShopCreationData, got " + creationData.getClass().getName());
	}
}
