package com.nisovin.shopkeepers.api.shopobjects.virtual;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s are not present in any world.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface VirtualShopObjectType<T extends VirtualShopObject> extends ShopObjectType<T> {
}
