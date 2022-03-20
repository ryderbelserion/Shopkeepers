package com.nisovin.shopkeepers.api.shopkeeper.admin;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.shopkeeper.ShopType;

/**
 * A {@link ShopType} that describes a type of {@link AdminShopkeeper}.
 *
 * @param <T>
 *            the type of admin shopkeeper that is described by this shop type
 */
public interface AdminShopType<T extends @NonNull AdminShopkeeper> extends ShopType<T> {
}
