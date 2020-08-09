package com.nisovin.shopkeepers.shopobjects.block;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;

/**
 * Produces the default {@link ShopObject#getId() shop object ids} used by {@link BlockShopObjectType block shop object
 * types}.
 * <p>
 * Even though there is no guarantee, it is recommended that block shop object types use these shared default object
 * ids, since it allows for quick lookup of the shopkeeper for a given block (without having to query all registered
 * block shop object types). Shop object types can indicate whether they uses these default ids via
 * {@link AbstractBlockShopObjectType#usesDefaultObjectIds()}.
 */
public class DefaultBlockShopObjectIds {

	private DefaultBlockShopObjectIds() {
	}

	public static String getObjectId(Block block) {
		if (block == null) return null;
		return getObjectId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public static String getObjectId(String worldName, int blockX, int blockY, int blockZ) {
		if (worldName == null) return null;
		return "block:" + worldName + "," + blockX + "," + blockY + "," + blockZ;
	}
}
