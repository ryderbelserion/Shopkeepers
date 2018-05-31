package com.nisovin.shopkeepers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.nisovin.shopkeepers.types.SelectableType;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractShopObjectType extends SelectableType implements ShopObjectType {

	protected AbstractShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	/**
	 * Creates a shop object.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param creationData
	 *            the used shop creation data, can be <code>null</code> (for ex. if the shopkeeper gets loaded)
	 * @return the shop object
	 */
	protected abstract ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData);

	@Override
	public abstract boolean needsSpawning();

	@Override
	public boolean isValidSpawnBlockFace(Block targetBlock, BlockFace targetBlockFace) {
		return (targetBlock != null) && (targetBlockFace != BlockFace.DOWN) && Utils.isBlockSide(targetBlockFace);
	}

	@Override
	public boolean isValidSpawnBlock(Block spawnBlock) {
		// TODO allow spawning inside of water?
		return (spawnBlock != null) && (spawnBlock.getType() == Material.AIR);
	}
}
