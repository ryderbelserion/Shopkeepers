package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;

public final class BookPlayerShopType extends AbstractPlayerShopType<SKBookPlayerShopkeeper> {

	public BookPlayerShopType() {
		super(
				"book",
				Collections.emptyList(),
				ShopkeepersPlugin.PLAYER_BOOK_PERMISSION,
				SKBookPlayerShopkeeper.class
		);
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
	public List<? extends String> getTradeSetupDescription() {
		return Messages.tradeSetupDescBook;
	}

	@Override
	protected SKBookPlayerShopkeeper createNewShopkeeper() {
		return new SKBookPlayerShopkeeper();
	}
}
