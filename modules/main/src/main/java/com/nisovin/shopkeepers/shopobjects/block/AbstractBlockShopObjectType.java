package com.nisovin.shopkeepers.shopobjects.block;

import java.util.List;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractBlockShopObjectType<T extends AbstractBlockShopObject> extends AbstractShopObjectType<T> implements BlockShopObjectType<T> {

	protected AbstractBlockShopObjectType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	/**
	 * Whether the shop objects of this type use the {@link DefaultBlockShopObjectIds default object ids}.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if the shop objects of this type use the default object ids
	 */
	public boolean usesDefaultObjectIds() {
		return true; // We use the default object ids by default
	}

	/**
	 * Gets the object id for the given {@link Block}.
	 * <p>
	 * This returns the {@link AbstractShopObject#getId() shop object id} that is or would be used by an active
	 * {@link ShopObject} of this type that represents the shopkeeper through the given block.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public Object getObjectId(Block block) {
		return this.getObjectId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Gets the object id for the given block coordinates.
	 * <p>
	 * This returns the {@link AbstractShopObject#getId() shop object id} that is or would be used by an active
	 * {@link ShopObject} of this type that represents the shopkeeper through the specified block.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the x coordinate
	 * @param blockY
	 *            the y coordinate
	 * @param blockZ
	 *            the z coordinate
	 * @return the object id, not <code>null</code>
	 */
	public Object getObjectId(String worldName, int blockX, int blockY, int blockZ) {
		// We use the default object ids by default:
		return DefaultBlockShopObjectIds.getObjectId(worldName, blockX, blockY, blockZ);
	}

	/**
	 * Gets the object id for the given block coordinates.
	 * <p>
	 * Unlike {@link #getObjectId(String, int, int, int)}, this may configure and reuse a shared object to represent the
	 * object id. Consequently, the returned object id is only valid until the next call to
	 * {@link #getSharedObjectId(String, int, int, int)}.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the block's x coordinate
	 * @param blockY
	 *            the block's y coordinate
	 * @param blockZ
	 *            the block's z coordinate
	 * @return the (shared) object id, not <code>null</code>
	 */
	protected Object getSharedObjectId(String worldName, int blockX, int blockY, int blockZ) {
		// We use the default object ids by default:
		return DefaultBlockShopObjectIds.getSharedObjectId(worldName, blockX, blockY, blockZ);
	}

	@Override
	public AbstractShopkeeper getShopkeeper(Block block) {
		return this.getShopkeeper(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	@Override
	public AbstractShopkeeper getShopkeeper(String worldName, int blockX, int blockY, int blockZ) {
		Object objectId = this.getSharedObjectId(worldName, blockX, blockY, blockZ);
		return this.getShopkeeperByObjectId(objectId);
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeper(block) != null);
	}

	@Override
	public boolean isShopkeeper(String worldName, int blockX, int blockY, int blockZ) {
		return (this.getShopkeeper(worldName, blockX, blockY, blockZ) != null);
	}
}
