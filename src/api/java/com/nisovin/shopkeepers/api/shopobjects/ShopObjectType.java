package com.nisovin.shopkeepers.api.shopobjects;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import com.nisovin.shopkeepers.api.types.SelectableType;

/**
 * Represents a certain type of {@link ShopObject}s.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface ShopObjectType<T extends ShopObject> extends SelectableType {

	/**
	 * Whether shop objects of this type get spawned with chunk loads and despawned with chunk unloads.
	 * 
	 * @return <code>true</code> if the shop objects of this type get spawned and despawned with chunk loads and unloads
	 */
	public boolean needsSpawning();

	/**
	 * Checks if the shopkeeper object can be spawned at the specified location.
	 * 
	 * @param spawnLocation
	 *            the spawn location
	 * @param targetedBlockFace
	 *            the block face against which to spawn the object, or <code>null</code> if unknown
	 * @return <code>true</code> if the shopkeeper object can be spawned there
	 */
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace);
}
