package com.nisovin.shopkeepers.api.shopobjects.block;

import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s use a {@link Block} to represent a
 * {@link Shopkeeper}.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface BlockShopObjectType<T extends BlockShopObject> extends ShopObjectType<T> {

	/**
	 * Gets the {@link Shopkeeper} that uses a {@link ShopObject} of this type and is currently
	 * represented by the given {@link Block}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the shopkeeper, or <code>null</code> if the given block is not a shopkeeper
	 *         currently, or if the corresponding shopkeeper is not using this type of shop object
	 */
	public @Nullable Shopkeeper getShopkeeper(Block block);

	/**
	 * Gets the {@link Shopkeeper} that uses a {@link ShopObject} of this type and is currently
	 * represented by the block at the specified location.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the block's x coordinate
	 * @param blockY
	 *            the block's y coordinate
	 * @param blockZ
	 *            the block's z coordinate
	 * @return the shopkeeper, or <code>null</code> if the specified block is not a shopkeeper
	 *         currently, or if the shopkeeper is not using this type of shop object
	 */
	public @Nullable Shopkeeper getShopkeeper(String worldName, int blockX, int blockY, int blockZ);

	/**
	 * Checks if the given block currently represents a {@link Shopkeeper} that uses a
	 * {@link ShopObject} of this type.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return <code>true</code> if the block is currently a shopkeeper that uses this type of shop
	 *         object
	 */
	public boolean isShopkeeper(Block block);

	/**
	 * Checks if the block at the specified location currently represents a {@link Shopkeeper} that
	 * uses a {@link ShopObject} of this type.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the block's x coordinate
	 * @param blockY
	 *            the block's y coordinate
	 * @param blockZ
	 *            the block's z coordinate
	 * @return <code>true</code> if the block is currently a shopkeeper that uses this type of shop
	 *         object
	 */
	public boolean isShopkeeper(String worldName, int blockX, int blockY, int blockZ);
}
