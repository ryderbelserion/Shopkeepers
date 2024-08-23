package com.nisovin.shopkeepers.api.shopobjects.citizens;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;

/**
 * A {@link ShopObjectType} of shop objects that use Citizens NPCs to represent the shopkeepers.
 *
 * @param <T>
 *            the type of the shop objects that this represents
 */
public interface CitizensShopObjectType<T extends CitizensShopObject>
		extends EntityShopObjectType<T> {
}
