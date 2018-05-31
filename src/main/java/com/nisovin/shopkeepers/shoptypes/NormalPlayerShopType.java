package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.util.Utils;

public class NormalPlayerShopType extends AbstractPlayerShopType<NormalPlayerShopkeeper> {

	NormalPlayerShopType() {
		super("player", ShopkeepersPlugin.PLAYER_NORMAL_PERMISSION);
	}

	@Override
	public NormalPlayerShopkeeper loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.validateConfigSection(config);
		NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper(config);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public NormalPlayerShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.validateCreationData(creationData);
		NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper((PlayerShopCreationData) creationData);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public String getCreatedMessage() {
		return Settings.msgPlayerShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("norm") || identifier.startsWith("sell");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedNormalShop);
	}
}
