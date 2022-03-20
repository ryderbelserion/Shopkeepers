package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A mutable {@link BlockLocation}.
 */
public final class MutableBlockLocation extends BlockLocation {

	/**
	 * Creates a new {@link MutableBlockLocation} for the given {@link Block}
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 */
	public static MutableBlockLocation of(Block block) {
		Validate.notNull(block, "block is null");
		return new MutableBlockLocation(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	/**
	 * Creates a new {@link MutableBlockLocation} for the given {@link Location}.
	 * <p>
	 * If the given location stores no world, this returns a block location with unset world name.
	 * If the given location stores a world, but the world has been unloaded by now, this throws an
	 * exception.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the location references a world that has been unloaded by now
	 */
	public static MutableBlockLocation of(Location location) {
		Validate.notNull(location, "location is null");
		// Note: We do not check Location#isWorldLoaded here, because if the location references a
		// world that has been unloaded by now, this is likely an error that we want to inform about
		// via an exception.
		// This throws an exception if the world has been unloaded:
		World world = location.getWorld();
		String worldName = (world != null) ? world.getName() : null;
		return new MutableBlockLocation(worldName,
				location.getBlockX(),
				location.getBlockY(),
				location.getBlockZ()
		);
	}

	/////

	/**
	 * Creates a new {@link MutableBlockLocation} with unset world name and all coordinates being
	 * zero.
	 */
	public MutableBlockLocation() {
		super();
	}

	/**
	 * Creates a new {@link MutableBlockLocation} with unset world name.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public MutableBlockLocation(int x, int y, int z) {
		super(x, y, z);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
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
	public MutableBlockLocation(@Nullable String worldName, int x, int y, int z) {
		super(worldName, x, y, z);
	}

	/**
	 * Creates a new {@link MutableBlockLocation} with unset world name.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using
	 * {@link BlockLocation#toBlock(double)}.
	 * 
	 * @param x
	 *            the precise x coordinate
	 * @param y
	 *            the precise y coordinate
	 * @param z
	 *            the precise z coordinate
	 */
	public MutableBlockLocation(double x, double y, double z) {
		super(x, y, z);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using
	 * {@link BlockLocation#toBlock(double)}.
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
	public MutableBlockLocation(@Nullable String worldName, double x, double y, double z) {
		super(worldName, x, y, z);
	}

	/**
	 * Sets this block location to the location of the given {@link Block}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public void set(Block block) {
		Validate.notNull(block, "block is null");
		this.set(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Sets this block location to the given {@link BlockLocation}.
	 * 
	 * @param blockLocation
	 *            the other block location, not <code>null</code>
	 */
	public void set(BlockLocation blockLocation) {
		Validate.notNull(blockLocation, "blockLocation is null");
		this.set(
				blockLocation.getWorldName(),
				blockLocation.getX(),
				blockLocation.getY(),
				blockLocation.getZ()
		);
	}

	/**
	 * Sets this block location to the block at the given {@link Location}.
	 * <p>
	 * If the given location stores no world, this returns a block location with unset world name.
	 * If the given location stores a world, but the world has been unloaded by now, this throws an
	 * exception.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the location references a world that has been unloaded by now
	 */
	public void set(Location location) {
		Validate.notNull(location, "location is null");
		// Note: We do not check Location#isWorldLoaded here, because if the location references a
		// world that has been unloaded by now, this is likely an error that we want to inform about
		// via an exception.
		// This throws an exception if the world has been unloaded:
		World world = location.getWorld();
		String worldName = (world != null) ? world.getName() : null;
		this.set(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	/**
	 * Sets this block location to the specified world name and coordinates.
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
	public void set(@Nullable String worldName, int x, int y, int z) {
		this.setWorldName(worldName);
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	@Override
	public void setWorldName(@Nullable String worldName) {
		super.setWorldName(worldName);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
	}

	@Override
	public void setZ(int z) {
		super.setZ(z);
	}

	@Override
	public BlockLocation immutable() {
		return new BlockLocation(this.getWorldName(), this.getX(), this.getY(), this.getZ());
	}
}
