package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public class BookPlayerShopType extends AbstractPlayerShopType<SKBookPlayerShopkeeper> {

	public BookPlayerShopType() {
		super("book", ShopkeepersPlugin.PLAYER_BOOK_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Settings.msgShopTypeBook;
	}

	@Override
	public String getDescription() {
		return Settings.msgShopTypeDescBook;
	}

	@Override
	public String getSetupDescription() {
		return Settings.msgShopSetupDescBook;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Settings.msgTradeSetupDescBook;
	}

	@Override
	public SKBookPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKBookPlayerShopkeeper shopkeeper = new SKBookPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKBookPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SKBookPlayerShopkeeper shopkeeper = new SKBookPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}
}
