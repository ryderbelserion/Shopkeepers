package com.nisovin.shopkeepers.util.bukkit;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.nisovin.shopkeepers.util.java.Validate;

public class LocationUtils {

	private LocationUtils() {
	}

	/**
	 * Checks if the given locations represent the same position.
	 * <p>
	 * This compares the locations' worlds, which are allowed to be both <code>null</code>, as well as their precise
	 * coordinates. This method ignores the pitch and yaw of the locations.
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
		if (!Objects.equals(location1.getWorld(), location2.getWorld())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getX()) != Double.doubleToLongBits(location2.getX())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getY()) != Double.doubleToLongBits(location2.getY())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getZ()) != Double.doubleToLongBits(location2.getZ())) {
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
}
