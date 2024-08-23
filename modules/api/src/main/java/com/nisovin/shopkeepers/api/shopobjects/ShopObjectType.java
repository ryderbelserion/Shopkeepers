package com.nisovin.shopkeepers.api.shopobjects;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.types.SelectableType;

/**
 * Represents a certain type of {@link ShopObject}s.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface ShopObjectType<T extends ShopObject> extends SelectableType {

	// Override to enforce that each subtype actually specifies a non-default display name
	@Override
	public abstract String getDisplayName();

	/**
	 * Checks if the shopkeeper object can be spawned at the specified location.
	 * 
	 * @param spawnLocation
	 *            the spawn location
	 * @param targetedBlockFace
	 *            the block face against which to spawn the object, or <code>null</code> if unknown
	 * @return <code>true</code> if the shopkeeper object can be spawned there
	 */
	public boolean isValidSpawnLocation(
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	);
}
