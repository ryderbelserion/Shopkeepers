package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.util.Utils;

public class BuyingPlayerShopType extends PlayerShopType<BuyingPlayerShopkeeper> {

	BuyingPlayerShopType() {
		super("buy", ShopkeepersAPI.PLAYER_BUY_PERMISSION);
	}

	@Override
	public BuyingPlayerShopkeeper loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.validateConfigSection(config);
		BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper(config);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public BuyingPlayerShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.validateCreationData(creationData);
		BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper((PlayerShopCreationData) creationData);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public String getCreatedMessage() {
		return Settings.msgBuyShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("buy");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedBuyShop);
	}
}
