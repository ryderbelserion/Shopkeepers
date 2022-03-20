package com.nisovin.shopkeepers.shopobjects;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.types.AbstractSelectableTypeRegistry;

public class SKShopObjectTypesRegistry
		extends AbstractSelectableTypeRegistry<@NonNull AbstractShopObjectType<?>>
		implements ShopObjectTypesRegistry<@NonNull AbstractShopObjectType<?>> {

	@Override
	protected String getTypeName() {
		return "shop object type";
	}
}
