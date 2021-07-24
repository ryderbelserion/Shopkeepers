package com.nisovin.shopkeepers.api.shopkeeper.player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopType;

/**
 * A {@link ShopType} that describes a type of {@link PlayerShopkeeper}.
 *
 * @param <T>
 *            the type of player shopkeeper that is described by this shop type
 */
public interface PlayerShopType<T extends PlayerShopkeeper> extends ShopType<T> {
}
