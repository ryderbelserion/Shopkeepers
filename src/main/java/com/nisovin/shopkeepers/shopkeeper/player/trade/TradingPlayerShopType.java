package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;
import com.nisovin.shopkeepers.util.StringUtils;

public class TradingPlayerShopType extends AbstractPlayerShopType<TradingPlayerShopkeeper> {

	public TradingPlayerShopType() {
		super("trade", ShopkeepersPlugin.PLAYER_TRADE_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Settings.msgShopTypeTrading;
	}

	@Override
	public String getDescription() {
		return Settings.msgShopTypeDescTrading;
	}

	@Override
	public String getSetupDescription() {
		return Settings.msgShopSetupDescTrading;
	}

	@Override
	public TradingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public TradingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("trad");
	}
}
