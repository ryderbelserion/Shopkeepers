package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public final class TradingPlayerShopType
		extends AbstractPlayerShopType<@NonNull SKTradingPlayerShopkeeper> {

	public TradingPlayerShopType() {
		super(
				"trade",
				Arrays.asList("trading"),
				ShopkeepersPlugin.PLAYER_TRADE_PERMISSION,
				SKTradingPlayerShopkeeper.class
		);
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
	public List<? extends @NonNull String> getTradeSetupDescription() {
		return Messages.tradeSetupDescTrading;
	}

	@Override
	protected SKTradingPlayerShopkeeper createNewShopkeeper() {
		return new SKTradingPlayerShopkeeper();
	}
}
