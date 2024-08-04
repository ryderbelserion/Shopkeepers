package com.nisovin.shopkeepers.shopkeeper;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.admin.regular.RegularAdminShopType;
import com.nisovin.shopkeepers.shopkeeper.player.book.BookPlayerShopType;
import com.nisovin.shopkeepers.shopkeeper.player.buy.BuyingPlayerShopType;
import com.nisovin.shopkeepers.shopkeeper.player.sell.SellingPlayerShopType;
import com.nisovin.shopkeepers.shopkeeper.player.trade.TradingPlayerShopType;

public class SKDefaultShopTypes implements DefaultShopTypes {

	private final RegularAdminShopType adminShopType = new RegularAdminShopType();
	private final SellingPlayerShopType sellingPlayerShopType = new SellingPlayerShopType();
	private final BuyingPlayerShopType buyingPlayerShopType = new BuyingPlayerShopType();
	private final TradingPlayerShopType tradingPlayerShopType = new TradingPlayerShopType();
	private final BookPlayerShopType bookPlayerShopType = new BookPlayerShopType();

	public SKDefaultShopTypes() {
	}

	@Override
	public List<? extends AbstractShopType<?>> getAll() {
		List<AbstractShopType<?>> shopTypes = new ArrayList<>();
		shopTypes.add(adminShopType);
		shopTypes.add(sellingPlayerShopType);
		shopTypes.add(buyingPlayerShopType);
		shopTypes.add(tradingPlayerShopType);
		shopTypes.add(bookPlayerShopType);
		return shopTypes;
	}

	@Override
	public RegularAdminShopType getAdminShopType() {
		return this.getRegularAdminShopType();
	}

	@Override
	public RegularAdminShopType getRegularAdminShopType() {
		return adminShopType;
	}

	@Override
	public SellingPlayerShopType getSellingPlayerShopType() {
		return sellingPlayerShopType;
	}

	@Override
	public BuyingPlayerShopType getBuyingPlayerShopType() {
		return buyingPlayerShopType;
	}

	@Override
	public TradingPlayerShopType getTradingPlayerShopType() {
		return tradingPlayerShopType;
	}

	@Override
	public BookPlayerShopType getBookPlayerShopType() {
		return bookPlayerShopType;
	}

	// STATIC ACCESSORS (for convenience)

	public static SKDefaultShopTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopTypes();
	}

	public static RegularAdminShopType ADMIN_REGULAR() {
		return getInstance().getRegularAdminShopType();
	}

	public static SellingPlayerShopType PLAYER_SELLING() {
		return getInstance().getSellingPlayerShopType();
	}

	public static BuyingPlayerShopType PLAYER_BUYING() {
		return getInstance().getBuyingPlayerShopType();
	}

	public static TradingPlayerShopType PLAYER_TRADING() {
		return getInstance().getTradingPlayerShopType();
	}

	public static BookPlayerShopType PLAYER_BOOK() {
		return getInstance().getBookPlayerShopType();
	}
}
