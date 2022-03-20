package com.nisovin.shopkeepers.api.shopobjects.virtual;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s are not present in any world.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface VirtualShopObjectType<T extends @NonNull VirtualShopObject>
		extends ShopObjectType<T> {
}
