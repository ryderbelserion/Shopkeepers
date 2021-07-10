package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;

public interface DefaultShopTypes {

	public List<? extends ShopType<?>> getAll();

	public ShopType<?> getAdminShopType();

	public PlayerShopType<?> getSellingPlayerShopType();

	public PlayerShopType<?> getBuyingPlayerShopType();

	public PlayerShopType<?> getTradingPlayerShopType();

	public PlayerShopType<?> getBookPlayerShopType();

	// STATICS (for convenience):

	public static DefaultShopTypes getInstance() {
		return ShopkeepersAPI.getPlugin().getDefaultShopTypes();
	}

	public static ShopType<?> ADMIN() {
		return getInstance().getAdminShopType();
	}

	public static PlayerShopType<?> PLAYER_SELLING() {
		return getInstance().getSellingPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_BUYING() {
		return getInstance().getBuyingPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_TRADING() {
		return getInstance().getTradingPlayerShopType();
	}

	public static PlayerShopType<?> PLAYER_BOOK() {
		return getInstance().getBookPlayerShopType();
	}
}
