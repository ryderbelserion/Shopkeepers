package com.nisovin.shopkeepers.shopobjects;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.BlockFaceUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class AbstractShopObjectType<T extends AbstractShopObject> extends AbstractSelectableType implements ShopObjectType<T> {

	protected AbstractShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractShopObjectType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Settings.msgSelectedShopObjectType,
				"{type}", this.getDisplayName());
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
	public abstract T createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData);

	@Override
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace) {
		// TODO check actual object size?
		if (spawnLocation == null || spawnLocation.getWorld() == null) return false;
		Block spawnBlock = spawnLocation.getBlock();
		if (!spawnBlock.isPassable()) return false;
		if (targetedBlockFace != null) {
			if (targetedBlockFace == BlockFace.DOWN || !BlockFaceUtils.isBlockSide(targetedBlockFace)) {
				return false;
			}
		}
		return true;
	}
}
