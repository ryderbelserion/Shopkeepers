package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;

public class SignShops {

	private final SKSignShopObjectType signShopObjectType = new SKSignShopObjectType(
			Unsafe.initialized(this)
	);
	private final SignShopListener signShopListener;

	public SignShops(SKShopkeepersPlugin plugin) {
		this.signShopListener = new SignShopListener(plugin, Unsafe.initialized(this));
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

	void cancelNextBlockPhysics(@Nullable Block block) {
		signShopListener.cancelNextBlockPhysics(block);
	}
}
