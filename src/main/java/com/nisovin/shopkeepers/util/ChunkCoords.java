package com.nisovin.shopkeepers.util;

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

	public final String worldName;
	public final int chunkX;
	public final int chunkZ;

	public ChunkCoords(String worldName, int chunkX, int chunkZ) {
		Validate.notNull(worldName);
		this.worldName = worldName;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public ChunkCoords(Chunk chunk) {
		Validate.notNull(chunk);
		this.worldName = chunk.getWorld().getName();
		this.chunkX = chunk.getX();
		this.chunkZ = chunk.getZ();
	}

	public ChunkCoords(Location location) {
		Validate.notNull(location);
		World world = location.getWorld();
		Validate.notNull(world);
		this.worldName = world.getName();
		this.chunkX = convertBlockCoord(location.getBlockX());
		this.chunkZ = convertBlockCoord(location.getBlockZ());
	}

	public ChunkCoords(Block block) {
		Validate.notNull(block);
		World world = block.getWorld();
		Validate.notNull(world);
		this.worldName = world.getName();
		this.chunkX = convertBlockCoord(block.getX());
		this.chunkZ = convertBlockCoord(block.getZ());
	}

	public boolean isChunkLoaded() {
		World world = Bukkit.getServer().getWorld(worldName);
		if (world != null) {
			return world.isChunkLoaded(chunkX, chunkZ);
		}
		return false;
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
}
