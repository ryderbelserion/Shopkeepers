package com.nisovin.shopkeepers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;
import com.nisovin.shopkeepers.util.Utils;

public abstract class ShopObjectType extends SelectableType {

	protected ShopObjectType(String identifier, String permission) {
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

	/**
	 * Whether or not shop objects of this type shall be spawned and despawned on chunk load and unload.
	 * 
	 * @return <code>true</code> if the shop object of this type shall be (de-)spawned together with chunk (un-)loads
	 */
	public abstract boolean needsSpawning();

	public boolean isValidSpawnBlockFace(Block targetBlock, BlockFace targetBlockFace) {
		assert targetBlock != null && Utils.isBlockSide(targetBlockFace);
		return (targetBlockFace != BlockFace.DOWN);
	}

	public boolean isValidSpawnBlock(Block spawnBlock) {
		assert spawnBlock != null;
		// TODO allow spawning inside of water?
		return spawnBlock.getType() == Material.AIR;
	}
}
