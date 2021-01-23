package com.nisovin.shopkeepers.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class MutableBlockLocation extends BlockLocation {

	/**
	 * The (invalid) dummy world name that is used to indicate that the world name is unset.
	 * <p>
	 * {@link BlockLocation} always expects a non-empty name, but this class additionally provides a state with an unset
	 * world name.
	 */
	public static final String UNSET_WORLD_NAME = "<UNSET>";

	/**
	 * Creates a new {@link MutableBlockLocation}.
	 * <p>
	 * The {@link #getWorldName() world name} is initialized to the {@link #UNSET_WORLD_NAME}, and the coordinates are
	 * all {@code zero}.
	 */
	public MutableBlockLocation() {
		super(UNSET_WORLD_NAME, 0, 0, 0);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
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
	public MutableBlockLocation(String worldName, int x, int y, int z) {
		super(worldName, x, y, z);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public MutableBlockLocation(Block block) {
		super(block);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
	 * <p>
	 * The given {@link Location} is expected to provide a {@link World}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 */
	public MutableBlockLocation(Location location) {
		super(location);
	}

	/**
	 * Creates a new {@link MutableBlockLocation}.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using {@link BlockLocation#toBlock(double)}.
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
	public MutableBlockLocation(String worldName, double x, double y, double z) {
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
		this.set(blockLocation.getWorldName(), blockLocation.getX(), blockLocation.getY(), blockLocation.getZ());
	}

	/**
	 * Sets this block location to the specified world name and coordinates.
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
	public void set(String worldName, int x, int y, int z) {
		this.setWorldName(worldName);
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	/**
	 * Sets the world name of this block location to the {@link #UNSET_WORLD_NAME}.
	 */
	public void unsetWorldName() {
		this.setWorldName(UNSET_WORLD_NAME);
	}

	/**
	 * Checks if the world name is not {@link #UNSET_WORLD_NAME}.
	 * 
	 * @return <code>true</code> if the world name is not unset
	 */
	public boolean hasWorldName() {
		return !this.getWorldName().equals(UNSET_WORLD_NAME);
	}

	@Override
	public void setWorldName(String worldName) {
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
}
