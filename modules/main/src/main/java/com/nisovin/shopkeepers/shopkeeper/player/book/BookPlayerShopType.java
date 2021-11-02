package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public final class BookPlayerShopType extends AbstractPlayerShopType<SKBookPlayerShopkeeper> {

	public BookPlayerShopType() {
		super("book", Collections.emptyList(), ShopkeepersPlugin.PLAYER_BOOK_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeBook;
	}

	@Override
	public String getDescription() {
		return Messages.shopTypeDescBook;
	}

	@Override
	public String getSetupDescription() {
		return Messages.shopSetupDescBook;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Messages.tradeSetupDescBook;
	}

	@Override
	public SKBookPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKBookPlayerShopkeeper shopkeeper = new SKBookPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKBookPlayerShopkeeper loadShopkeeper(ShopkeeperData shopkeeperData) throws ShopkeeperCreateException {
		this.validateShopkeeperData(shopkeeperData);
		return new SKBookPlayerShopkeeper(shopkeeperData);
	}
}
