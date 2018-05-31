package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shoptypes.DefaultShopTypes;

public class SKDefaultShopTypes implements DefaultShopTypes {

	private final AdminShopType adminShopType = new AdminShopType();
	private final NormalPlayerShopType normalPlayerShopType = new NormalPlayerShopType();
	private final BuyingPlayerShopType buyingPlayerShopType = new BuyingPlayerShopType();
	private final TradingPlayerShopType tradingPlayerShopType = new TradingPlayerShopType();
	private final BookPlayerShopType bookPlayerShopType = new BookPlayerShopType();

	public SKDefaultShopTypes() {
	}

	@Override
	public List<AbstractShopType<?>> getAllShopTypes() {
		List<AbstractShopType<?>> shopTypes = new ArrayList<>();
		shopTypes.add(adminShopType);
		shopTypes.add(normalPlayerShopType);
		shopTypes.add(buyingPlayerShopType);
		shopTypes.add(tradingPlayerShopType);
		shopTypes.add(bookPlayerShopType);
		return shopTypes;
	}

	@Override
	public AdminShopType getAdminShopType() {
		return adminShopType;
	}

	@Override
	public NormalPlayerShopType getNormalPlayerShopType() {
		return normalPlayerShopType;
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

	// STATICS (for convenience):

	public static SKDefaultShopTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopTypes();
	}

	public static AdminShopType ADMIN() {
		return getInstance().getAdminShopType();
	}

	public static NormalPlayerShopType PLAYER_NORMAL() {
		return getInstance().getNormalPlayerShopType();
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
