package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.block.Block;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;

public class SignShops {

	private final SKSignShopObjectType signShopObjectType = new SKSignShopObjectType(this);
	private final SignShopListener signShopListener;

	public SignShops(SKShopkeepersPlugin plugin) {
		this.signShopListener = new SignShopListener(plugin, this);
	}

	public void onEnable() {
		if (Settings.enableSignShops) {
			signShopListener.onEnable();
		}
	}

	public void onDisable() {
		signShopListener.onDisable();
	}

	public SKSignShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	void cancelNextBlockPhysics(Block block) {
		signShopListener.cancelNextBlockPhysics(block);
	}
}
