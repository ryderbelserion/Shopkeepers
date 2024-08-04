package com.nisovin.shopkeepers.shopobjects.block;

import java.util.List;

import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopobjects.block.BlockShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractBlockShopObjectType<T extends AbstractBlockShopObject>
		extends AbstractShopObjectType<T> implements BlockShopObjectType<T> {

	protected AbstractBlockShopObjectType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<@NonNull T> shopObjectType
	) {
		super(identifier, aliases, permission, shopObjectType);
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeper(Block block) {
		return this.getShopkeeper(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ());
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeper(
			String worldName,
			int blockX,
			int blockY,
			int blockZ
	) {
		Object objectId = BlockShopObjectIds.getSharedObjectId(worldName, blockX, blockY, blockZ);
		return this.getShopkeeperByObjectId(objectId);
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeper(block) != null);
	}

	@Override
	public boolean isShopkeeper(String worldName, int blockX, int blockY, int blockZ) {
		return (this.getShopkeeper(worldName, blockX, blockY, blockZ) != null);
	}
}
