package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public class SellingPlayerShopType extends AbstractPlayerShopType<SellingPlayerShopkeeper> {

	public SellingPlayerShopType() {
		super("sell", Arrays.asList("selling", "normal", "player"), ShopkeepersPlugin.PLAYER_SELL_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Settings.msgShopTypeSelling;
	}

	@Override
	public String getDescription() {
		return Settings.msgShopTypeDescSelling;
	}

	@Override
	public String getSetupDescription() {
		return Settings.msgShopSetupDescSelling;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Settings.msgTradeSetupDescSelling;
	}

	@Override
	public SellingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SellingPlayerShopkeeper shopkeeper = new SellingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SellingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SellingPlayerShopkeeper shopkeeper = new SellingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}
}
