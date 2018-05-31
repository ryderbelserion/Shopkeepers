package com.nisovin.shopkeepers.shopobjects;

import java.util.List;

import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectTypes;

public interface DefaultShopObjectTypes {

	public List<? extends ShopObjectType> getAllObjectTypes();

	public LivingEntityObjectTypes getLivingEntityObjectTypes();

	public ShopObjectType getSignShopObjectType();

	public ShopObjectType getCitizensShopObjectType();

	// STATICS (for convenience):

	public static DefaultShopObjectTypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultShopObjectTypes();
	}

	public static LivingEntityObjectTypes MOBS() {
		return getInstance().getLivingEntityObjectTypes();
	}

	public static ShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static ShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
