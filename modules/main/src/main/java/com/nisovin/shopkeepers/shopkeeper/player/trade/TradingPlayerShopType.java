package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public final class TradingPlayerShopType extends AbstractPlayerShopType<SKTradingPlayerShopkeeper> {

	public TradingPlayerShopType() {
		super("trade", Arrays.asList("trading"), ShopkeepersPlugin.PLAYER_TRADE_PERMISSION,
				SKTradingPlayerShopkeeper.class);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeTrading;
	}

	@Override
	public String getDescription() {
		return Messages.shopTypeDescTrading;
	}

	@Override
	public String getSetupDescription() {
		return Messages.shopSetupDescTrading;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Messages.tradeSetupDescTrading;
	}

	@Override
	public SKTradingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKTradingPlayerShopkeeper shopkeeper = new SKTradingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKTradingPlayerShopkeeper loadShopkeeper(ShopkeeperData shopkeeperData) throws ShopkeeperCreateException {
		this.validateShopkeeperData(shopkeeperData);
		return new SKTradingPlayerShopkeeper(shopkeeperData);
	}
}
