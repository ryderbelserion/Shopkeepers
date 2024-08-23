package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public final class BuyingPlayerShopType extends AbstractPlayerShopType<SKBuyingPlayerShopkeeper> {

	public BuyingPlayerShopType() {
		super(
				"buy",
				Arrays.asList("buying"),
				ShopkeepersPlugin.PLAYER_BUY_PERMISSION,
				SKBuyingPlayerShopkeeper.class
		);
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
	public List<? extends String> getTradeSetupDescription() {
		return Messages.tradeSetupDescBuying;
	}

	@Override
	protected SKBuyingPlayerShopkeeper createNewShopkeeper() {
		return new SKBuyingPlayerShopkeeper();
	}
}
