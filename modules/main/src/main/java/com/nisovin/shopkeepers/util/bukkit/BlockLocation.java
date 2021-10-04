package com.nisovin.shopkeepers.util.bukkit;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Combination of a world name with block coordinates.
 * <p>
 * The world name can also be {@link #hasWorldName() unset}.
 * <p>
 * This type is immutable, but has mutable subclasses such as {@link MutableBlockLocation}. Use {@link #immutable()} to
 * get a block location that is guaranteed to be immutable.
 */
public class BlockLocation {

	/**
	 * An immutable block location with unset world name and all coordinates being zero.
	 */
	public static final BlockLocation ZERO = new BlockLocation();

	/**
	 * Creates a new {@link BlockLocation} for the given {@link Block}
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 */
	public static BlockLocation of(Block block) {
		Validate.notNull(block, "block is null");
		return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Creates a new {@link BlockLocation} for the given {@link Location}.
	 * <p>
	 * If the given location stores no world, this returns a block location with unset world name. If the given location
	 * stores a world, but the world has been unloaded by now, this throws an exception.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the location references a world that has been unloaded by now
	 */
	public static BlockLocation of(Location location) {
		Validate.notNull(location, "location is null");
		// Note: We no not check Location#isWorldLoaded here, because if the location references a world that has been
		// unloaded by now, this is likely an unexpected error that we want to inform about via an exception.
		World world = location.getWorld(); // Throws an exception if the world has been unloaded
		String worldName = (world != null) ? world.getName() : null;
		return new BlockLocation(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	/**
	 * Converts a precise double coordinate to an integer block coordinate.
	 * 
	 * @param coordinate
	 *            the double coordinate
	 * @return the block coordinate
	 */
	public static int toBlock(double coordinate) {
		return Location.locToBlock(coordinate);
	}

	/////

	private String worldName; // Not empty, but can be null
	private int x;
	private int y;
	private int z;

	/**
	 * Creates a new {@link BlockLocation} with unset world name and all coordinates being zero.
	 */
	public BlockLocation() {
		this(null, 0, 0, 0);
	}

	/**
	 * Creates a new {@link BlockLocation} with unset world name.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public BlockLocation(int x, int y, int z) {
		this(null, x, y, z);
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public BlockLocation(String worldName, int x, int y, int z) {
		validateWorldName(worldName);
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Creates a new {@link BlockLocation} with unset world name.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using {@link #toBlock(double)}.
	 * 
	 * @param x
	 *            the precise x coordinate
	 * @param y
	 *            the precise y coordinate
	 * @param z
	 *            the precise z coordinate
	 */
	public BlockLocation(double x, double y, double z) {
		this(null, x, y, z);
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using {@link #toBlock(double)}.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 * @param x
	 *            the precise x coordinate
	 * @param y
	 *            the precise y coordinate
	 * @param z
	 *            the precise z coordinate
	 */
	public BlockLocation(String worldName, double x, double y, double z) {
		this(worldName, toBlock(x), toBlock(y), toBlock(z));
	}

	private static void validateWorldName(String worldName) {
		Validate.isTrue(worldName == null || !worldName.isEmpty(), "worldName is empty");
	}

	/**
	 * Gets the world name.
	 * 
	 * @return the non-empty world name, or <code>null</code> if the world name is not set
	 */
	public final String getWorldName() {
		return worldName;
	}

	/**
	 * Checks if the world name is set (i.e. is not <code>null</code>).
	 * 
	 * @return <code>true</code> if the world name is set
	 */
	public final boolean hasWorldName() {
		return (worldName != null);
	}

	/**
	 * Sets the world name.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 */
	protected void setWorldName(String worldName) {
		validateWorldName(worldName);
		this.worldName = worldName;
	}

	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public final int getX() {
		return x;
	}

	/**
	 * Sets the x coordinate.
	 * 
	 * @param x
	 *            the x coordinate
	 */
	protected void setX(int x) {
		this.x = x;
	}

	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public final int getY() {
		return y;
	}

	/**
	 * Sets the y coordinate.
	 * 
	 * @param y
	 *            the y coordinate
	 */
	protected void setY(int y) {
		this.y = y;
	}

	/**
	 * Gets the z coordinate.
	 * 
	 * @return the block's z coordinate
	 */
	public final int getZ() {
		return z;
	}

	/**
	 * Sets the z coordinate.
	 * 
	 * @param z
	 *            the z coordinate
	 */
	protected void setZ(int z) {
		this.z = z;
	}

	/**
	 * Gets the {@link World} this block location is located in.
	 * 
	 * @return the world, or <code>null</code> if the world name of this block location is unset or if the world is not
	 *         loaded currently
	 */
	public final World getWorld() {
		if (!this.hasWorldName()) return null;
		return Bukkit.getWorld(worldName);
	}

	/**
	 * Gets the {@link Block} that corresponds to this location.
	 * 
	 * @return the block, or <code>null</code> if the world name of this block location is unset or if the world is not
	 *         loaded currently
	 */
	public final Block getBlock() {
		World world = this.getWorld();
		if (world == null) return null;
		return world.getBlockAt(x, y, z); // Not null (even for coordinates outside the world's bounds)
	}

	/**
	 * Checks if this {@link BlockLocation} matches the specified world name and coordinates.
	 * 
	 * @param worldName
	 *            the world name
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return <code>true</code> if the world name and coordinates match
	 */
	public final boolean matches(String worldName, int x, int y, int z) {
		if (this.x != x) return false;
		if (this.y != y) return false;
		if (this.z != z) return false;
		// Checked last for performance reasons:
		if (!Objects.equals(this.worldName, worldName)) return false;
		return true;
	}

	/**
	 * Checks if this {@link BlockLocation} matches the location of the given {@link Block}
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the locations match
	 */
	public final boolean matches(Block block) {
		if (block == null) return false;
		return this.matches(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Creates a mutable copy of this block location.
	 * 
	 * @return the mutable copy, not <code>null</code>
	 */
	public final MutableBlockLocation mutableCopy() {
		return new MutableBlockLocation(worldName, x, y, z);
	}

	/**
	 * Gets an immutable instance of this block location.
	 * <p>
	 * If this block location is already immutable, this returns the block location itself. Otherwise, an immutable copy
	 * is returned.
	 * 
	 * @return the immutable block location, not <code>null</code>
	 */
	public BlockLocation immutable() {
		// This type is assumed to be immutable. Mutable subclasses need to override this method.
		return this;
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [worldName=");
		builder.append(worldName);
		builder.append(", x=");
		builder.append(x);
		builder.append(", y=");
		builder.append(y);
		builder.append(", z=");
		builder.append(z);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldName.hashCode();
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BlockLocation)) return false;
		BlockLocation other = (BlockLocation) obj;
		return this.matches(other.worldName, other.x, other.y, other.z);
	}
}
