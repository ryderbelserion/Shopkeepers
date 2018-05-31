package com.nisovin.shopkeepers.shoptypes;

import java.util.List;

import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;

public interface DefaultShopTypes {

	public List<? extends ShopType<?>> getAllShopTypes();

	public ShopType<?> getAdminShopType();

	public PlayerShopType<?> getNormalPlayerShopType();

	public PlayerShopType<?> getTradingPlayerShopType();

	public PlayerShopType<?> getBuyingPlayerShopType();

	public PlayerShopType<?> getBookPlayerShopType();

	// STATICS (for convenience):

	public static DefaultShopTypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes();
	}

	public static ShopType<?> ADMIN() {
		return getInstance().getAdminShopType();
	}

	public static PlayerShopType<?> PLAYER_NORMAL() {
		return getInstance().getNormalPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_TRADING() {
		return getInstance().getTradingPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_BUYING() {
		return getInstance().getBuyingPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_BOOK() {
		return getInstance().getBookPlayerShopType();
	}
}
