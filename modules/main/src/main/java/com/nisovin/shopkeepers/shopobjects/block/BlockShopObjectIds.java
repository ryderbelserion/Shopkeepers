package com.nisovin.shopkeepers.shopobjects.block;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObject;
import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Produces the {@link AbstractShopObject#getId() shop object ids} that are used by
 * {@link BlockShopObject}s.
 * <p>
 * All shop objects that represent their shopkeepers by blocks are required to use these common shop
 * object ids. This ensures that lookups of the shopkeeper for a given block are quick and do not
 * require to individually query all registered {@link BlockShopObjectType}s.
 */
public final class BlockShopObjectIds {

	/**
	 * Gets the object id that is or would be used by a {@link BlockShopObject} that represents the
	 * shopkeeper by the given {@link Block}.
	 * 
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public static Object getObjectId(Block block) {
		Validate.notNull(block, "block is null");
		return getObjectId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Gets the object id that is or would be used by a {@link BlockShopObject} that represents the
	 * shopkeeper by the block at the specified coordinates.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the block's x coordinate
	 * @param blockY
	 *            the block's y coordinate
	 * @param blockZ
	 *            the block's z coordinate
	 * @return the object id, not <code>null</code>
	 */
	public static Object getObjectId(String worldName, int blockX, int blockY, int blockZ) {
		Validate.notEmpty(worldName, "worldName is null or empty");
		// Note: We could also use the block itself as id. However, by using a separate type of
		// object that is under our control, we can slightly improve the performance of lookups by
		// block coordinates by reusing a shared BlockLocation instance. Otherwise, we would need to
		// retrieve a new Block object for every lookup. This optimization is for example useful
		// when we handle the high frequency BlockPhysicsEvent.
		// We expect that a specific block represents at most one shopkeeper, regardless of the
		// actual block shop object type.
		return new BlockLocation(worldName, blockX, blockY, blockZ);
	}

	// Note: We do not need to reset the shared block location after use. Its attributes are
	// overwritten with subsequent use, and it does not hold on to any costly resources.
	private static final MutableBlockLocation sharedBlockLocation = new MutableBlockLocation();

	/**
	 * Gets the object id for the given {@link Block}.
	 * <p>
	 * Unlike {@link #getObjectId(Block)}, this may configure and reuse a shared object to represent
	 * the object id. Consequently, the returned object id is only valid until the next call to
	 * {@link #getSharedObjectId(Block)} or {@link #getSharedObjectId(String, int, int, int)}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the (shared) object id, not <code>null</code>
	 */
	public static Object getSharedObjectId(Block block) {
		Validate.notNull(block, "block is null");
		return getSharedObjectId(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	/**
	 * Gets the object id for the given block coordinates.
	 * <p>
	 * Unlike {@link #getObjectId(String, int, int, int)}, this may configure and reuse a shared
	 * object to represent the object id. Consequently, the returned object id is only valid until
	 * the next call to {@link #getSharedObjectId(Block)} or
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
	public static Object getSharedObjectId(String worldName, int blockX, int blockY, int blockZ) {
		Validate.notEmpty(worldName, "worldName is null or empty");
		sharedBlockLocation.set(worldName, blockX, blockY, blockZ);
		return sharedBlockLocation;
	}

	private BlockShopObjectIds() {
	}
}
