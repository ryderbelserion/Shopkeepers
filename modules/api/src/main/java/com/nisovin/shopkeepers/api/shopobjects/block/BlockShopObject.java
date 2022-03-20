package com.nisovin.shopkeepers.api.shopobjects.block;

import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;

/**
 * A {@link ShopObject} that uses a {@link Block} to represent a {@link Shopkeeper} in the world.
 */
public interface BlockShopObject extends ShopObject {

	/**
	 * Gets the block.
	 * 
	 * @return the block, or <code>null</code> if the shop object is not spawned currently
	 */
	public @Nullable Block getBlock();
}
