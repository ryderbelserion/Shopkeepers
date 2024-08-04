package com.nisovin.shopkeepers.shopobjects;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.types.AbstractSelectableTypeRegistry;

public class SKShopObjectTypesRegistry
		extends AbstractSelectableTypeRegistry<AbstractShopObjectType<?>>
		implements ShopObjectTypesRegistry<AbstractShopObjectType<?>> {

	@Override
	protected String getTypeName() {
		return "shop object type";
	}
}
