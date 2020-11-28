package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

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

	public AbstractShopkeeper getSignShop(Block block) {
		if (block == null) return null;
		return this.getSignShop(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	AbstractShopkeeper getSignShop(String worldName, int blockX, int blockY, int blockZ) {
		String objectId = signShopObjectType.createObjectId(worldName, blockX, blockY, blockZ);
		AbstractShopkeeper shopkeeper = plugin.getShopkeeperRegistry().getActiveShopkeeper(objectId);
		if (shopkeeper != null && shopkeeper.getShopObject() instanceof SKSignShopObject) {
			return shopkeeper;
		} else {
			return null;
		}
	}

	public boolean isSignShop(Block block) {
		return (this.getSignShop(block) != null);
	}

	boolean isSignShop(String worldName, int blockX, int blockY, int blockZ) {
		return (this.getSignShop(worldName, blockX, blockY, blockZ) != null);
	}

	void cancelNextBlockPhysics(Block block) {
		signShopListener.cancelNextBlockPhysics(block);
	}
}
