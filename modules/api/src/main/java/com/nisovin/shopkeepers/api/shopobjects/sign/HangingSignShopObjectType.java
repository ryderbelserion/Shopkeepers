package com.nisovin.shopkeepers.api.shopobjects.sign;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;

/**
 * A {@link ShopObjectType} of shop objects that use hanging signs to represent the shopkeepers.
 *
 * @param <T>
 *            the type of the shop objects that this represents
 */
public interface HangingSignShopObjectType<T extends @NonNull HangingSignShopObject>
		extends BlockShopObjectType<T> {
}
