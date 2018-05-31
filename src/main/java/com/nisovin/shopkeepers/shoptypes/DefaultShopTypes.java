package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.AbstractShopType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;

public class DefaultShopTypes {

	private final AbstractShopType<?> adminShopType = new AdminShopType();
	private final AbstractPlayerShopType<?> normalPlayerShopType = new NormalPlayerShopType();
	private final AbstractPlayerShopType<?> tradingPlayerShopType = new TradingPlayerShopType();
	private final AbstractPlayerShopType<?> buyingPlayerShopType = new BuyingPlayerShopType();
	private final AbstractPlayerShopType<?> bookPlayerShopType = new BookPlayerShopType();

	public DefaultShopTypes() {
	}

	public List<AbstractShopType<?>> getAllShopTypes() {
		List<AbstractShopType<?>> shopTypes = new ArrayList<>();
		shopTypes.add(adminShopType);
		shopTypes.add(normalPlayerShopType);
		shopTypes.add(tradingPlayerShopType);
		shopTypes.add(buyingPlayerShopType);
		shopTypes.add(bookPlayerShopType);
		return shopTypes;
	}

	public AbstractShopType<?> getAdminShopType() {
		return adminShopType;
	}

	public AbstractPlayerShopType<?> getNormalPlayerShopType() {
		return normalPlayerShopType;
	}

	public AbstractPlayerShopType<?> getTradingPlayerShopType() {
		return tradingPlayerShopType;
	}

	public AbstractPlayerShopType<?> getBuyingPlayerShopType() {
		return buyingPlayerShopType;
	}

	public AbstractPlayerShopType<?> getBookPlayerShopType() {
		return bookPlayerShopType;
	}

	// STATICS (for convenience):

	public static AbstractShopType<?> ADMIN() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getAdminShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_NORMAL() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getNormalPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_TRADING() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getTradingPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_BUYING() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getBuyingPlayerShopType();
	}

	public static AbstractPlayerShopType<?> PLAYER_BOOK() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getBookPlayerShopType();
	}
}
