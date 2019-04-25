package com.nisovin.shopkeepers.shopobjects.block;

import java.util.List;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractBlockShopObjectType<T extends AbstractBlockShopObject> extends AbstractShopObjectType<T> implements BlockShopObjectType<T> {

	protected AbstractBlockShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractBlockShopObjectType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	public String createObjectId(Block block) {
		if (block == null) return null;
		// inline for performance:
		return this.getIdentifier() + ":" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
	}
}
