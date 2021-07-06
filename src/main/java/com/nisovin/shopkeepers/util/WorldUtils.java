package com.nisovin.shopkeepers.util;

import java.util.Set;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class WorldUtils {

	private WorldUtils() {
	}

	// Temporary objects getting re-used during ray tracing:
	private static final Location TEMP_START_LOCATION = new Location(null, 0, 0, 0);
	private static final Vector TEMP_START_POSITION = new Vector();
	private static final Vector DOWN_DIRECTION = new Vector(0.0D, -1.0D, 0.0D);
	private static final double RAY_TRACE_OFFSET = 0.01D;

	/**
	 * Get the distance to the nearest block collision in the range of the given <code>maxDistance</code>.
	 * <p>
	 * This performs a ray trace through the blocks' collision boxes, ignoring passable blocks and optionally ignoring
	 * specific types of fluids.
	 * <p>
	 * The ray tracing gets slightly offset (by <code>0.01</code>) in order to make sure that we don't miss any block
	 * directly at the start location. If this results in a hit above the start location, we ignore it and return
	 * <code>0.0</code>.
	 * 
	 * @param startLocation
	 *            the start location, has to use a valid world, does not get modified
	 * @param maxDistance
	 *            the max distance to check for block collisions, has to be positive
	 * @param collidableFluids
	 *            the types of fluids to collide with
	 * @return the distance to the ground, or <code>maxDistance</code> if there are no block collisions within the
	 *         specified range
	 */
	public static double getCollisionDistanceToGround(Location startLocation, double maxDistance, Set<Material> collidableFluids) {
		assert collidableFluids != null;
		World world = startLocation.getWorld();
		assert world != null;
		// Setup re-used offset start location:
		TEMP_START_LOCATION.setWorld(world);
		TEMP_START_LOCATION.setX(startLocation.getX());
		TEMP_START_LOCATION.setY(startLocation.getY() + RAY_TRACE_OFFSET);
		TEMP_START_LOCATION.setZ(startLocation.getZ());

		TEMP_START_POSITION.setX(TEMP_START_LOCATION.getX());
		TEMP_START_POSITION.setY(TEMP_START_LOCATION.getY());
		TEMP_START_POSITION.setZ(TEMP_START_LOCATION.getZ());

		double offsetMaxDistance = maxDistance + RAY_TRACE_OFFSET;

		RayTraceResult rayTraceResult = null;
		if (collidableFluids.isEmpty()) {
			// Considers block collision boxes, ignoring passable blocks and fluids (null if there is not hit):
			rayTraceResult = world.rayTraceBlocks(TEMP_START_LOCATION, DOWN_DIRECTION, offsetMaxDistance, FluidCollisionMode.NEVER, true);
		} else {
			// Take the given types of fluids into account, but still ignore other types of passable blocks:
			int offsetMaxDistanceBlocks = NumberConversions.ceil(offsetMaxDistance);
			BlockIterator blockIterator = new BlockIterator(world, TEMP_START_POSITION, DOWN_DIRECTION, 0.0D, offsetMaxDistanceBlocks);
			while (blockIterator.hasNext()) {
				Block block = blockIterator.next();
				if (!block.isPassable() || collidableFluids.contains(block.getType())) {
					rayTraceResult = block.rayTrace(TEMP_START_LOCATION, DOWN_DIRECTION, offsetMaxDistance, FluidCollisionMode.ALWAYS);
					if (rayTraceResult != null) {
						break;
					} // Else: The raytrace did not collide with the block (eg. open trap doors, etc.)
				} // Else: Continue.
			}
			// rayTraceResult can remain null if there are no block collisions in range.
		}
		TEMP_START_LOCATION.setWorld(null); // Cleanup temporarily used start location

		double distanceToGround;
		if (rayTraceResult == null) {
			// No collision with the range:
			distanceToGround = maxDistance;
		} else {
			distanceToGround = TEMP_START_POSITION.distance(rayTraceResult.getHitPosition()) - RAY_TRACE_OFFSET;
			// Might be negative if the hit is between the start location and the offset start location.
			// We ignore it then.
			if (distanceToGround < 0.0D) distanceToGround = 0.0D;
		}
		return distanceToGround;
	}
}
