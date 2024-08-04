package com.nisovin.shopkeepers.shopkeeper.admin;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractAdminShopType<T extends AbstractAdminShopkeeper>
		extends AbstractShopType<T> implements AdminShopType<T> {

	protected AbstractAdminShopType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<T> shopkeeperType
	) {
		super(identifier, aliases, permission, shopkeeperType);
	}

	@Override
	protected void validateCreationData(ShopCreationData shopCreationData) {
		super.validateCreationData(shopCreationData);
		Validate.isTrue(shopCreationData instanceof AdminShopCreationData,
				() -> "shopCreationData is not of type " + AdminShopCreationData.class.getName()
						+ ", but: " + shopCreationData.getClass().getName());
	}
}
