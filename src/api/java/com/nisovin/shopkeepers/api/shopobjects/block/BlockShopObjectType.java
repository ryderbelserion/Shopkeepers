package com.nisovin.shopkeepers.api.shopobjects.block;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s use a {@link Block} to represent a {@link Shopkeeper}.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface BlockShopObjectType<T extends BlockShopObject> extends ShopObjectType<T> {
}
