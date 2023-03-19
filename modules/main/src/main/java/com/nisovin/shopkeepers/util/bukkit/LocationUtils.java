package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	 * Gets the squared distance between the given locations.
	 * <p>
	 * Both locations are required to have a valid (non-<code>null</code>) world. If the locations
	 * are located in different worlds, this returns {@link Double#MAX_VALUE}.
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
		// Comparing the worlds by identity rather than equals / UUID is sufficient. Location itself
		// compares worlds by identity in Location#distanceSquared. Also, even if the worlds have
		// the same UUID but are different instances, it is unclear whether they can be treated as
		// equal, since some operations on a previously unloaded world might have no effect.
		if (world1 != world2) return Double.MAX_VALUE; // Different worlds

		// Note: Not using Location#distanceSquared to avoid redundant precondition checks.
		double dx = location1.getX() - location2.getX();
		double dy = location1.getY() - location2.getY();
		double dz = location1.getZ() - location2.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the squared distance between the given locations.
	 * <p>
	 * Unlike {@link #getDistanceSquared(Location, Location)}, this does not throw an exception, but
	 * returns {@link Double#MAX_VALUE} if at least one of the locations is <code>null</code>, or if
	 * one of the locations' worlds is not loaded or <code>null</code>.
	 * 
	 * @param location1
	 *            the first location
	 * @param location2
	 *            the second location
	 * @return the squared distance
	 */
	public static double getSafeDistanceSquared(
			@Nullable Location location1,
			@Nullable Location location2
	) {
		if (location1 == null || location2 == null) return Double.MAX_VALUE;
		if (!location1.isWorldLoaded() || !location2.isWorldLoaded()) return Double.MAX_VALUE;

		World world1 = location1.getWorld();
		World world2 = location2.getWorld();
		if (world1 == null || world1 != world2) return Double.MAX_VALUE;

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
