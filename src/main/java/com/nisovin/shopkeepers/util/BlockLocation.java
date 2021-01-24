package com.nisovin.shopkeepers.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockLocation {

	/**
	 * Converts a precise (double) coordinate to a block (int) coordinate.
	 * 
	 * @param coordinate
	 *            the precise coordinate
	 * @return the block coordinate
	 */
	public static int toBlock(double coordinate) {
		return Location.locToBlock(coordinate);
	}

	private String worldName; // Not null or empty
	private int x;
	private int y;
	private int z;

	/**
	 * Creates a new {@link BlockLocation}.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public BlockLocation(String worldName, int x, int y, int z) {
		Validate.notEmpty(worldName, "worldName is empty");
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public BlockLocation(Block block) {
		this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * <p>
	 * The given {@link Location} is expected to provide a {@link World}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 */
	public BlockLocation(Location location) {
		// Throws an exception if the Location is null or does not provide a world:
		this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using {@link #toBlock(double)}.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
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

	/**
	 * Gets the world name.
	 * 
	 * @return the world name, not <code>null</code> or empty
	 */
	public String getWorldName() {
		return worldName;
	}

	/**
	 * Sets the world name.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 */
	protected void setWorldName(String worldName) {
		Validate.notEmpty(worldName, "worldName is empty");
		this.worldName = worldName;
	}

	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public int getX() {
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
	public int getY() {
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
	public int getZ() {
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
	 * Gets the {@link World} this block location is located in, or <code>null</code> if the world is not loaded.
	 * 
	 * @return the world, or <code>null</code> if it is not loaded
	 */
	public World getWorld() {
		return Bukkit.getWorld(worldName);
	}

	/**
	 * Gets the {@link Block} associated with this location, or <code>null</code> if the world is not loaded.
	 * 
	 * @return the block, or <code>null</code> if the world is not loaded
	 */
	public Block getBlock() {
		World world = this.getWorld();
		if (world == null) return null;
		return world.getBlockAt(x, y, z); // Not null (even for coordinates outside the world's bounds)
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldName.hashCode();
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BlockLocation)) return false;
		BlockLocation other = (BlockLocation) obj;
		return this.matches(other.worldName, other.x, other.y, other.z);
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
	public boolean matches(String worldName, int x, int y, int z) {
		if (this.x != x) return false;
		if (this.y != y) return false;
		if (this.z != z) return false;
		// Checked last for performance reasons:
		if (!this.worldName.equals(worldName)) return false;
		return true;
	}

	/**
	 * Checks if this {@link BlockLocation} matches the location of the given {@link Block}
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the locations match
	 */
	public boolean matches(Block block) {
		if (block == null) return false;
		return this.matches(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	@Override
	public String toString() {
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
}
