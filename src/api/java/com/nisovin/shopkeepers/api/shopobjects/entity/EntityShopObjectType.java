package com.nisovin.shopkeepers.api.shopobjects.entity;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s use an {@link Entity} to represent a {@link Shopkeeper}.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface EntityShopObjectType<T extends EntityShopObject> extends ShopObjectType<T> {
}
