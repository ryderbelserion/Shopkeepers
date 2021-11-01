package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObjectType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;

public final class SKSignShopObjectType extends AbstractBlockShopObjectType<SKSignShopObject> implements SignShopObjectType<SKSignShopObject> {

	private final SignShops signShops;

	public SKSignShopObjectType(SignShops signShops) {
		super("sign", Collections.emptyList(), "shopkeeper.sign");
		this.signShops = signShops;
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableSignShops;
	}

	@Override
	public String getDisplayName() {
		return Messages.shopObjectTypeSign;
	}

	@Override
	public boolean mustBeSpawned() {
		return true; // Despawn signs on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace) {
		// Block has to be empty, and limit to wall sign faces if sign posts are disabled:
		return spawnLocation.getBlock().isEmpty()
				&& (Settings.enableSignPostShops || targetedBlockFace != BlockFace.UP)
				&& super.isValidSpawnLocation(spawnLocation, targetedBlockFace);
	}

	@Override
	public SKSignShopObject createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new SKSignShopObject(signShops, shopkeeper, creationData);
	}
}
