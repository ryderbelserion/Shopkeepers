package com.nisovin.shopkeepers.api.shopkeeper.admin;

import com.nisovin.shopkeepers.api.shopkeeper.ShopType;

/**
 * A {@link ShopType} that describes a type of {@link AdminShopkeeper}.
 *
 * @param <T>
 *            the type of admin shopkeeper that is described by this shop type
 */
public interface AdminShopType<T extends AdminShopkeeper> extends ShopType<T> {
}
