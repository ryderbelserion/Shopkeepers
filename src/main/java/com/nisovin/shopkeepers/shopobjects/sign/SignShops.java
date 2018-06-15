package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;

public class SignShops {

	private final SKShopkeepersPlugin plugin;
	private final SignShopObjectType signShopObjectType = new SignShopObjectType(this);
	private final SignShopListener signShopListener;

	public SignShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		signShopListener = new SignShopListener(plugin);
	}

	public void onEnable() {
		if (Settings.enableSignShops) {
			Bukkit.getPluginManager().registerEvents(signShopListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(signShopListener);
	}

	public SignShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	void cancelNextBlockPhysics(Location location) {
		signShopListener.cancelNextBlockPhysics(location);
	}
}
