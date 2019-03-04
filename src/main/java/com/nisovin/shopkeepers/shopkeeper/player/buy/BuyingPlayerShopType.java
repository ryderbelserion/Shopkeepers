package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;
import com.nisovin.shopkeepers.util.StringUtils;

public class BuyingPlayerShopType extends AbstractPlayerShopType<BuyingPlayerShopkeeper> {

	public BuyingPlayerShopType() {
		super("buy", ShopkeepersPlugin.PLAYER_BUY_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Settings.msgShopTypeBuying;
	}

	@Override
	public String getDescription() {
		return Settings.msgShopTypeDescBuying;
	}

	@Override
	public String getSetupDescription() {
		return Settings.msgShopSetupDescBuying;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Settings.msgTradeSetupDescBuying;
	}

	@Override
	public BuyingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public BuyingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("buy");
	}
}
