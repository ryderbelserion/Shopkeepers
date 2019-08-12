package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public class BuyingPlayerShopType extends AbstractPlayerShopType<SKBuyingPlayerShopkeeper> {

	public BuyingPlayerShopType() {
		super("buy", Arrays.asList("buying"), ShopkeepersPlugin.PLAYER_BUY_PERMISSION);
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
	public SKBuyingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKBuyingPlayerShopkeeper shopkeeper = new SKBuyingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKBuyingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SKBuyingPlayerShopkeeper shopkeeper = new SKBuyingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}
}
