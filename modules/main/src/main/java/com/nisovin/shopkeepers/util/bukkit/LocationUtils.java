package com.nisovin.shopkeepers.util.bukkit;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.nisovin.shopkeepers.util.java.Validate;

public final class LocationUtils {

	/**
	 * Gets the {@link World} of the given {@link Location}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 * @return the world, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the location has no world or the world has been unloaded
	 */
	public static World getWorld(Location location) {
		Validate.notNull(location, "location is null");
		// Throws an exception if the world is no longer loaded:
		World world = location.getWorld();
		return Validate.notNull(world, "location's world is null");
	}

	/**
	 * Checks if the given locations represent the same position.
	 * <p>
	 * This compares the precise coordinates of the given locations, as well as their worlds. If both locations do not
	 * store any world, or both their worlds are no longer loaded, their worlds are considered equal. The pitch and yaw
	 * of the given locations are ignored.
	 * <p>
	 * If both locations are <code>null</code>, this returns <code>true</code>.
	 * 
	 * @param location1
	 *            the first location
	 * @param location2
	 *            the second location
	 * @return <code>true</code> if the locations correspond to the same position
	 */
	public static boolean isEqualPosition(Location location1, Location location2) {
		if (location1 == location2) return true; // Also handles both being null
		if (location1 == null || location2 == null) return false;
		if (Double.doubleToLongBits(location1.getX()) != Double.doubleToLongBits(location2.getX())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getY()) != Double.doubleToLongBits(location2.getY())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getZ()) != Double.doubleToLongBits(location2.getZ())) {
			return false;
		}
		boolean world1Loaded = location1.isWorldLoaded();
		boolean world2Loaded = location2.isWorldLoaded();
		if (world1Loaded != world2Loaded) {
			return false;
		}
		if (world1Loaded && !Objects.equals(location1.getWorld(), location2.getWorld())) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the squared distance between the given locations.
	 * <p>
	 * Both locations are required to have a valid (non-<code>null</code>) world. If the locations are located in
	 * different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param location1
	 *            the first location, not <code>null</code>
	 * @param location2
	 *            the second location, not <code>null</code>
	 * @return the squared distance
	 */
	public static double getDistanceSquared(Location location1, Location location2) {
		Validate.notNull(location1, "location1 is null");
		Validate.notNull(location2, "location2 is null");
		// These throw an exception if the worlds are no longer loaded:
		World world1 = location1.getWorld();
		World world2 = location2.getWorld();
		Validate.notNull(world1, "World of location1 is null");
		Validate.notNull(world2, "World of location2 is null");
		// Comparing the worlds by identity rather than equals / UUID is sufficient. Location itself compares worlds by
		// identity in Location#distanceSquared. Also, even if the worlds have the same UUID but are different
		// instances, it is unclear whether they can be treated as equal, since some operations on a previously unloaded
		// world might have no effect.
		if (world1 != world2) return Double.MAX_VALUE; // Different worlds
		// Note: Not using Location#distanceSquared to avoid redundant precondition checks.
		double dx = location1.getX() - location2.getX();
		double dy = location1.getY() - location2.getY();
		double dz = location1.getZ() - location2.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the block's center location.
	 * 
	 * @param block
	 *            the block
	 * @return the block's center location
	 */
	public static Location getBlockCenterLocation(Block block) {
		Validate.notNull(block, "block is null");
		return block.getLocation().add(0.5D, 0.5D, 0.5D);
	}

	private LocationUtils() {
	}
}
