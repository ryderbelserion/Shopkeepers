package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;

public class SignShops {

	private final SKShopkeepersPlugin plugin;
	private final SKSignShopObjectType signShopObjectType = new SKSignShopObjectType(this);
	private final SignShopListener signShopListener;

	public SignShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		signShopListener = new SignShopListener(this);
	}

	public void onEnable() {
		if (Settings.enableSignShops) {
			Bukkit.getPluginManager().registerEvents(signShopListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(signShopListener);
	}

	public SKSignShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	void cancelNextBlockPhysics(Block block) {
		signShopListener.cancelNextBlockPhysics(block);
	}
}
