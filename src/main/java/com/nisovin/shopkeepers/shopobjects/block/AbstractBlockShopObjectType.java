package com.nisovin.shopkeepers.shopobjects.block;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractBlockShopObjectType<T extends AbstractBlockShopObject> extends AbstractShopObjectType<T> implements BlockShopObjectType<T> {

	protected AbstractBlockShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	public String createObjectId(Block block) {
		if (block == null) return null;
		return this.getIdentifier() + ":" + Utils.getLocationString(block);
	}
}
