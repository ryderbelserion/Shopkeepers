package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.util.java.Validate;

public class MutableChunkCoords extends ChunkCoords {

	/**
	 * The (invalid) dummy world name that is used to indicate that the world name is unset.
	 * <p>
	 * {@link ChunkCoords} always expects a non-empty name, but this class additionally provides a
	 * state with an unset world name.
	 */
	public static final String UNSET_WORLD_NAME = "<UNSET>";

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * <p>
	 * The {@link #getWorldName() world name} is initialized to the {@link #UNSET_WORLD_NAME}, and
	 * the chunk coordinates are all {@code zero}.
	 */
	public MutableChunkCoords() {
		super(UNSET_WORLD_NAME, 0, 0);
	}

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param chunkX
	 *            the chunk's x coordinate
	 * @param chunkZ
	 *            the chunk's z coordinate
	 */
	public MutableChunkCoords(String worldName, int chunkX, int chunkZ) {
		super(worldName, chunkX, chunkZ);
	}

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * 
	 * @param chunk
	 *            the chunk, not <code>null</code>
	 */
	public MutableChunkCoords(Chunk chunk) {
		super(chunk);
	}

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * 
	 * @param chunkCoords
	 *            the chunk coordinates to copy, not <code>null</code>
	 */
	public MutableChunkCoords(ChunkCoords chunkCoords) {
		super(chunkCoords);
	}

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * <p>
	 * The given {@link Location} is expected to provide a {@link World}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 */
	public MutableChunkCoords(Location location) {
		super(location);
	}

	/**
	 * Creates a new {@link MutableChunkCoords}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public MutableChunkCoords(Block block) {
		super(block);
	}

	/**
	 * Sets this {@link MutableChunkCoords} to the coordinates of the given chunk.
	 * 
	 * @param chunk
	 *            the chunk, not <code>null</code>
	 */
	public void set(Chunk chunk) {
		Validate.notNull(chunk, "chunk is null");
		this.set(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	/**
	 * Sets this {@link MutableChunkCoords} to the chunk coordinates of the given {@link Block}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public void set(Block block) {
		Validate.notNull(block, "block is null");
		this.set(block.getWorld().getName(), fromBlock(block.getX()), fromBlock(block.getZ()));
	}

	/**
	 * Sets this {@link MutableChunkCoords} to the chunk coordinates of the given {@link Location}.
	 * <p>
	 * The location is expected to provide a {@link World}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 */
	public void set(Location location) {
		this.set(
				LocationUtils.getWorld(location).getName(),
				fromBlock(location.getBlockX()),
				fromBlock(location.getBlockZ())
		);
	}

	/**
	 * Sets this {@link MutableChunkCoords} to the given {@link ChunkCoords}.
	 * 
	 * @param chunkCoords
	 *            the other chunk coordinates, not <code>null</code>
	 */
	public void set(ChunkCoords chunkCoords) {
		Validate.notNull(chunkCoords, "chunkCoords is null");
		this.set(chunkCoords.getWorldName(), chunkCoords.getChunkX(), chunkCoords.getChunkZ());
	}

	/**
	 * Sets this {@link MutableChunkCoords} to the specified world name and chunk coordinates.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param chunkX
	 *            the chunk's x coordinate
	 * @param chunkZ
	 *            the chunk's z coordinate
	 */
	public void set(String worldName, int chunkX, int chunkZ) {
		this.setWorldName(worldName);
		this.setChunkX(chunkX);
		this.setChunkZ(chunkZ);
	}

	/**
	 * Sets the world name of this {@link MutableChunkCoords} to the {@link #UNSET_WORLD_NAME}.
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
	public void setChunkX(int chunkX) {
		super.setChunkX(chunkX);
	}

	@Override
	public void setChunkZ(int chunkZ) {
		super.setChunkZ(chunkZ);
	}
}
