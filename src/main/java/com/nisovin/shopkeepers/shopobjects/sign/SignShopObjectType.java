package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class SignShopObjectType extends AbstractShopObjectType<SignShop> {

	private final SignShops signShops;

	public SignShopObjectType(SignShops signShops) {
		super("sign", "shopkeeper.sign");
		this.signShops = signShops;
	}

	@Override
	public SignShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new SignShop(signShops, shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableSignShops;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
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
		// limit to wall sign faces:
		return (targetBlockFace != BlockFace.UP) && super.isValidSpawnBlockFace(targetBlock, targetBlockFace);
	}
}
