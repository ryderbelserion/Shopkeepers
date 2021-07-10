package com.nisovin.shopkeepers.shopobjects.block;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;

/**
 * Produces the default {@link AbstractShopObject#getId() shop object ids} used by {@link BlockShopObjectType block shop
 * object types}.
 * <p>
 * Even though there this is not guaranteed or required, it is recommended that block shop object types use these shared
 * default object ids, since it allows for a quick lookup of the shopkeeper for a given block (without having to query
 * all registered block shop object types individually). Shop object types can indicate whether they uses these default
 * ids via {@link AbstractBlockShopObjectType#usesDefaultObjectIds()}.
 */
public class DefaultBlockShopObjectIds {

	private DefaultBlockShopObjectIds() {
	}

	/**
	 * Gets the default object id for the given {@link Block}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public static Object getObjectId(Block block) {
		return getObjectId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Gets the default object id for the given block coordinates.
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
		// Note: We could also use the block itself as id. However, using a separate type of object that is under our
		// control allows us to slightly improve the performance of lookups by block coordinates by reusing a shared
		// object (which is required when we handle the BlockPhysicsEvent), since it doesn't require us to retrieve a
		// new Block object.
		// We expect that a specific block represents at most one shopkeeper (regardless of the actual block shop object
		// type).
		return new BlockLocation(worldName, blockX, blockY, blockZ);
	}

	// Note: We do not need to reset the shared block location after use. Its properties get overwritten with subsequent
	// use, and it is not holding on to any costly resources.
	private static final MutableBlockLocation sharedBlockLocation = new MutableBlockLocation();

	/**
	 * Gets the default object id for the given {@link Block}.
	 * <p>
	 * Unlike {@link #getObjectId(Block)}, this may configure and reuse a shared object to represent the object id.
	 * Consequently, the returned object id is only valid until the next call to {@link #getSharedObjectId(Block)} or
	 * {@link #getSharedObjectId(String, int, int, int)}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the (shared) object id, not <code>null</code>
	 */
	public static Object getSharedObjectId(Block block) {
		return getSharedObjectId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Gets the default object id for the given block coordinates.
	 * <p>
	 * Unlike {@link #getObjectId(String, int, int, int)}, this may configure and reuse a shared object to represent the
	 * object id. Consequently, the returned object id is only valid until the next call to
	 * {@link #getSharedObjectId(Block)} or {@link #getSharedObjectId(String, int, int, int)}.
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
		sharedBlockLocation.set(worldName, blockX, blockY, blockZ);
		return sharedBlockLocation;
	}
}
