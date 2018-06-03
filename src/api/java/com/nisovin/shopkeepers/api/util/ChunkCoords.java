package com.nisovin.shopkeepers.api.util;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Stores positional information about a chunk, like its world and coordinates.
 */
public final class ChunkCoords {

	private final String worldName;
	private final int chunkX;
	private final int chunkZ;

	public ChunkCoords(String worldName, int chunkX, int chunkZ) {
		Validate.notNull(worldName, "World name is null!");
		this.worldName = worldName;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public ChunkCoords(Chunk chunk) {
		Validate.notNull(chunk, "Chunk is null!");
		this.worldName = chunk.getWorld().getName();
		this.chunkX = chunk.getX();
		this.chunkZ = chunk.getZ();
	}

	public ChunkCoords(Location location) {
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world);
		this.worldName = world.getName();
		this.chunkX = convertBlockCoord(location.getBlockX());
		this.chunkZ = convertBlockCoord(location.getBlockZ());
	}

	public ChunkCoords(Block block) {
		Validate.notNull(block, "Block is null!");
		World world = block.getWorld();
		Validate.notNull(world);
		this.worldName = world.getName();
		this.chunkX = convertBlockCoord(block.getX());
		this.chunkZ = convertBlockCoord(block.getZ());
	}

	/**
	 * Gets the chunk's world name.
	 * 
	 * @return the chunk's world name
	 */
	public String getWorldName() {
		return worldName;
	}

	/**
	 * Gets the chunk's x-coordinate
	 * 
	 * @return the chunk's x-coordinate
	 */
	public int getChunkX() {
		return chunkX;
	}

	/**
	 * Gets the chunk's z-coordinate
	 * 
	 * @return the chunk's z-coordinate
	 */
	public int getChunkZ() {
		return chunkZ;
	}

	public boolean isChunkLoaded() {
		World world = Bukkit.getServer().getWorld(worldName);
		if (world != null) {
			return world.isChunkLoaded(chunkX, chunkZ);
		}
		return false;
	}

	public boolean isSameChunk(Chunk chunk) {
		if (chunk == null) return false;
		return chunk.getX() == chunkX && chunk.getZ() == chunkZ && chunk.getWorld().getName().equals(worldName);
	}

	@Override
	public String toString() {
		return getClass().getName()
				+ "[worldName=" + worldName
				+ ",chunkX=" + chunkX
				+ ",chunkZ=" + chunkZ + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chunkX;
		result = prime * result + chunkZ;
		result = prime * result + worldName.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		ChunkCoords other = (ChunkCoords) obj;
		if (chunkX != other.chunkX) return false;
		if (chunkZ != other.chunkZ) return false;
		if (!worldName.equals(other.worldName)) return false;
		return true;
	}

	public static int convertBlockCoord(int blockCoord) {
		return blockCoord >> 4;
	}

	public static ChunkCoords fromBlockPos(String worldName, int blockX, int blockZ) {
		return new ChunkCoords(worldName, convertBlockCoord(blockX), convertBlockCoord(blockZ));
	}

	public static boolean isSameChunk(Location loc1, Location loc2) {
		if (loc1 == null || loc2 == null) return false;
		World world1 = loc1.getWorld();
		World world2 = loc2.getWorld();
		if (world1 == null || world2 == null) return false;
		if (!world1.getName().equals(world2.getName())) return false;

		int chunkX1 = convertBlockCoord(loc1.getBlockX());
		int chunkX2 = convertBlockCoord(loc2.getBlockX());
		if (chunkX1 != chunkX2) return false;

		int chunkZ1 = convertBlockCoord(loc1.getBlockZ());
		int chunkZ2 = convertBlockCoord(loc2.getBlockZ());
		if (chunkZ1 != chunkZ2) return false;
		return true;
	}

	public static boolean isChunkLoaded(Location location) {
		if (location == null) return false;
		World world = location.getWorld();
		if (world == null) return false;
		return world.isChunkLoaded(convertBlockCoord(location.getBlockX()), convertBlockCoord(location.getBlockZ()));
	}
}
