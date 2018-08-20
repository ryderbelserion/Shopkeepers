package com.nisovin.shopkeepers.shopkeeper.admin;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;

public abstract class AbstractAdminShopType<T extends AbstractAdminShopkeeper> extends AbstractShopType<T> implements AdminShopType<T> {

	protected AbstractAdminShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	// common functions that might be useful for sub-classes:

	@Override
	protected void validateCreationData(ShopCreationData shopCreationData) {
		super.validateCreationData(shopCreationData);
		Validate.isTrue(shopCreationData instanceof AdminShopCreationData,
				"Expecting AdminShopCreationData, got " + shopCreationData.getClass().getName());
	}
}
