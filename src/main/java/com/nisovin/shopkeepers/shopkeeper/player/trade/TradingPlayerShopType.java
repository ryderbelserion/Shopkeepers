package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public class TradingPlayerShopType extends AbstractPlayerShopType<SKTradingPlayerShopkeeper> {

	public TradingPlayerShopType() {
		super("trade", Arrays.asList("trading"), ShopkeepersPlugin.PLAYER_TRADE_PERMISSION);
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
	public List<String> getTradeSetupDescription() {
		return Settings.msgTradeSetupDescTrading;
	}

	@Override
	public SKTradingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKTradingPlayerShopkeeper shopkeeper = new SKTradingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKTradingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SKTradingPlayerShopkeeper shopkeeper = new SKTradingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}
}
