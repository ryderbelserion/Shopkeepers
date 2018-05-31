package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.AbstractShopType;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shoptypes.DefaultShopTypes;

public class SKDefaultShopTypes implements DefaultShopTypes {

	private final AbstractShopType<?> adminShopType = new AdminShopType();
	private final AbstractPlayerShopType<?> normalPlayerShopType = new NormalPlayerShopType();
	private final AbstractPlayerShopType<?> tradingPlayerShopType = new TradingPlayerShopType();
	private final AbstractPlayerShopType<?> buyingPlayerShopType = new BuyingPlayerShopType();
	private final AbstractPlayerShopType<?> bookPlayerShopType = new BookPlayerShopType();

	public SKDefaultShopTypes() {
	}

	@Override
	public List<AbstractShopType<?>> getAllShopTypes() {
		List<AbstractShopType<?>> shopTypes = new ArrayList<>();
		shopTypes.add(adminShopType);
		shopTypes.add(normalPlayerShopType);
		shopTypes.add(tradingPlayerShopType);
		shopTypes.add(buyingPlayerShopType);
		shopTypes.add(bookPlayerShopType);
		return shopTypes;
	}

	@Override
	public AbstractShopType<?> getAdminShopType() {
		return adminShopType;
	}

	@Override
	public AbstractPlayerShopType<?> getNormalPlayerShopType() {
		return normalPlayerShopType;
	}

	@Override
	public AbstractPlayerShopType<?> getTradingPlayerShopType() {
		return tradingPlayerShopType;
	}

	@Override
	public AbstractPlayerShopType<?> getBuyingPlayerShopType() {
		return buyingPlayerShopType;
	}

	@Override
	public AbstractPlayerShopType<?> getBookPlayerShopType() {
		return bookPlayerShopType;
	}

	// STATICS (for convenience):

	public static SKDefaultShopTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopTypes();
	}

	public static AbstractShopType<?> ADMIN() {
		return getInstance().getAdminShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_NORMAL() {
		return getInstance().getNormalPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_TRADING() {
		return getInstance().getTradingPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_BUYING() {
		return getInstance().getBuyingPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_BOOK() {
		return getInstance().getBookPlayerShopType();
	}
}
