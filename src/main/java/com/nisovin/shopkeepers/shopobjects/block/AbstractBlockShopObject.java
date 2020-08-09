package com.nisovin.shopkeepers.shopobjects.block;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;

public abstract class AbstractBlockShopObject extends AbstractShopObject implements BlockShopObject {

	protected AbstractBlockShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
	}

	@Override
	public abstract AbstractBlockShopObjectType<?> getType();

	@Override
	public boolean isActive() {
		return (this.getBlock() != null);
	}

	@Override
	public String getId() {
		// Returns null if the block is null:
		return this.getType().createObjectId(this.getBlock());
	}
}
