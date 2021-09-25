package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public class BuyingPlayerShopType extends AbstractPlayerShopType<SKBuyingPlayerShopkeeper> {

	public BuyingPlayerShopType() {
		super("buy", Arrays.asList("buying"), ShopkeepersPlugin.PLAYER_BUY_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeBuying;
	}

	@Override
	public String getDescription() {
		return Messages.shopTypeDescBuying;
	}

	@Override
	public String getSetupDescription() {
		return Messages.shopSetupDescBuying;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Messages.tradeSetupDescBuying;
	}

	@Override
	public SKBuyingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKBuyingPlayerShopkeeper shopkeeper = new SKBuyingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKBuyingPlayerShopkeeper loadShopkeeper(int id, ShopkeeperData shopkeeperData) throws ShopkeeperCreateException {
		this.validateShopkeeperData(shopkeeperData);
		SKBuyingPlayerShopkeeper shopkeeper = new SKBuyingPlayerShopkeeper(id, shopkeeperData);
		return shopkeeper;
	}
}
