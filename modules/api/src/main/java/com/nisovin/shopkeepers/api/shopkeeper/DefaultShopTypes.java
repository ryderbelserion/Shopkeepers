package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;

/**
 * Provides access to the default {@link ShopType ShopTypes}.
 */
public interface DefaultShopTypes {

	/**
	 * Gets all default {@link ShopType ShopTypes}.
	 * 
	 * @return all default shop types
	 */
	public List<? extends ShopType<?>> getAll();

	/**
	 * Gets the default regular {@link AdminShopType}.
	 * 
	 * @return the default regular {@link AdminShopType}
	 * @deprecated Use {@link #getRegularAdminShopType()} instead
	 */
	@Deprecated
	public ShopType<?> getAdminShopType();

	/**
	 * Gets the default regular {@link AdminShopType}.
	 * 
	 * @return the default regular {@link AdminShopType}
	 */
	public AdminShopType<?> getRegularAdminShopType();

	/**
	 * Gets the default selling {@link PlayerShopType}.
	 * 
	 * @return the default selling {@link PlayerShopType}
	 */
	public PlayerShopType<?> getSellingPlayerShopType();

	/**
	 * Gets the default buying {@link PlayerShopType}.
	 * 
	 * @return the default buying {@link PlayerShopType}
	 */
	public PlayerShopType<?> getBuyingPlayerShopType();

	/**
	 * Gets the default trading {@link PlayerShopType}.
	 * 
	 * @return the default trading {@link PlayerShopType}
	 */
	public PlayerShopType<?> getTradingPlayerShopType();

	/**
	 * Gets the default book selling {@link PlayerShopType}.
	 * 
	 * @return the default book selling {@link PlayerShopType}
	 */
	public PlayerShopType<?> getBookPlayerShopType();

	// STATIC ACCESSORS (for convenience)

	/**
	 * Gets the {@link DefaultShopTypes} instance.
	 * 
	 * @return the instance
	 */
	public static DefaultShopTypes getInstance() {
		return ShopkeepersAPI.getPlugin().getDefaultShopTypes();
	}

	/**
	 * Gets the default regular {@link AdminShopType}.
	 * 
	 * @return the default regular {@link AdminShopType}
	 * @deprecated Use {@link #ADMIN_REGULAR()} instead
	 */
	@Deprecated
	public static ShopType<?> ADMIN() {
		return ADMIN_REGULAR();
	}

	/**
	 * Gets the default regular {@link AdminShopType}.
	 * 
	 * @return the default regular {@link AdminShopType}
	 * @see #getRegularAdminShopType()
	 */
	public static AdminShopType<?> ADMIN_REGULAR() {
		return getInstance().getRegularAdminShopType();
	}

	/**
	 * Gets the default selling {@link PlayerShopType}.
	 * 
	 * @return the default selling {@link PlayerShopType}
	 * @see #getSellingPlayerShopType()
	 */
	public static PlayerShopType<?> PLAYER_SELLING() {
		return getInstance().getSellingPlayerShopType();
	}

	/**
	 * Gets the default buying {@link PlayerShopType}.
	 * 
	 * @return the default buying {@link PlayerShopType}
	 * @see #getBuyingPlayerShopType()
	 */
	public static PlayerShopType<?> PLAYER_BUYING() {
		return getInstance().getBuyingPlayerShopType();
	}

	/**
	 * Gets the default trading {@link PlayerShopType}.
	 * 
	 * @return the default trading {@link PlayerShopType}
	 * @see #getTradingPlayerShopType()
	 */
	public static PlayerShopType<?> PLAYER_TRADING() {
		return getInstance().getTradingPlayerShopType();
	}

	/**
	 * Gets the default book selling {@link PlayerShopType}.
	 * 
	 * @return the default book selling {@link PlayerShopType}
	 * @see #getBookPlayerShopType()
	 */
	public static PlayerShopType<?> PLAYER_BOOK() {
		return getInstance().getBookPlayerShopType();
	}
}
