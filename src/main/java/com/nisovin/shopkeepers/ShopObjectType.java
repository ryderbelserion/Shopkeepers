package com.nisovin.shopkeepers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.nisovin.shopkeepers.types.SelectableType;

public interface ShopObjectType extends SelectableType {

	/**
	 * Whether shop objects of this type get spawned with chunk loads and despawned with chunk unloads.
	 * 
	 * @return <code>true</code> if the shop objects of this type get spawned and despawned with chunk loads and unloads
	 */
	public boolean needsSpawning();

	public boolean isValidSpawnBlockFace(Block targetBlock, BlockFace targetBlockFace);

	public boolean isValidSpawnBlock(Block spawnBlock);
}
