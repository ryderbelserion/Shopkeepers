package com.nisovin.shopkeepers.api.shopobjects;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.sign.HangingSignShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObjectType;

/**
 * Provides access to the default {@link ShopObjectType ShopObjectTypes}.
 */
public interface DefaultShopObjectTypes {

	/**
	 * Gets all default {@link ShopObjectType ShopObjectTypes}.
	 * 
	 * @return all default shop objects types
	 */
	public List<? extends ShopObjectType<?>> getAll();

	/**
	 * Gets the {@link LivingShopObjectTypes}, which provides access to the default shop object
	 * types that use mobs to represent the shopkeeper.
	 * 
	 * @return the {@link LivingShopObjectTypes}
	 */
	public LivingShopObjectTypes getLivingShopObjectTypes();

	/**
	 * Gets the default {@link ShopObjectType} of sign shopkeepers.
	 * 
	 * @return the default sign shop object type
	 */
	public SignShopObjectType<?> getSignShopObjectType();

	/**
	 * Gets the default {@link ShopObjectType} of hanging sign shopkeepers.
	 * 
	 * @return the default hanging sign shop object type
	 */
	public HangingSignShopObjectType<?> getHangingSignShopObjectType();

	/**
	 * Gets the default {@link CitizensShopObjectType}.
	 * 
	 * @return the default {@link CitizensShopObjectType}
	 */
	public CitizensShopObjectType<?> getCitizensShopObjectType();

	// STATIC ACCESSORS (for convenience)

	/**
	 * Gets the {@link DefaultShopObjectTypes} instance.
	 * 
	 * @return the instance
	 */
	public static DefaultShopObjectTypes getInstance() {
		return ShopkeepersAPI.getPlugin().getDefaultShopObjectTypes();
	}

	/**
	 * Gets the {@link LivingShopObjectTypes}, which provides access to the default shop object
	 * types that use mobs to represent the shopkeeper.
	 * 
	 * @return the {@link LivingShopObjectTypes}
	 * @see #getLivingShopObjectTypes()
	 */
	public static LivingShopObjectTypes LIVING() {
		return getInstance().getLivingShopObjectTypes();
	}

	/**
	 * Gets the default {@link ShopObjectType} of sign shopkeepers.
	 * 
	 * @return the default sign shop object type
	 * @see #getSignShopObjectType()
	 */
	public static SignShopObjectType<?> SIGN() {
		return getInstance().getSignShopObjectType();
	}

	/**
	 * Gets the default {@link ShopObjectType} of hanging sign shopkeepers.
	 * 
	 * @return the default hanging sign shop object type
	 * @see #getHangingSignShopObjectType()
	 */
	public static HangingSignShopObjectType<?> HANGING_SIGN() {
		return getInstance().getHangingSignShopObjectType();
	}

	/**
	 * Gets the default {@link CitizensShopObjectType}.
	 * 
	 * @return the default {@link CitizensShopObjectType}
	 * @see #getCitizensShopObjectType()
	 */
	public static CitizensShopObjectType<?> CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
