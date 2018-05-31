package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.util.Utils;

public class SignShopObjectType extends ShopObjectType {

	SignShopObjectType() {
		super("block", "shopkeeper.sign");
	}

	@Override
	protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
		return new SignShop(shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableSignShops;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("sign");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedSignShop);
	}

	@Override
	public boolean needsSpawning() {
		return false; // TODO maybe cleanup the shop signs on chunk unload in the future?
	}

	@Override
	public boolean isValidSpawnBlockFace(Block targetBlock, BlockFace targetBlockFace) {
		assert targetBlock != null && Utils.isBlockSide(targetBlockFace);
		// limit to wall sign faces:
		return (targetBlockFace != BlockFace.DOWN && targetBlockFace != BlockFace.UP);
	}
}
