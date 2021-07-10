package com.nisovin.shopkeepers.api.shopobjects;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObjectType;

public interface DefaultShopObjectTypes {

	public List<? extends ShopObjectType<?>> getAll();

	public LivingShopObjectTypes getLivingShopObjectTypes();

	public SignShopObjectType<?> getSignShopObjectType();

	public CitizensShopObjectType<?> getCitizensShopObjectType();

	// STATICS (for convenience):

	public static DefaultShopObjectTypes getInstance() {
		return ShopkeepersAPI.getPlugin().getDefaultShopObjectTypes();
	}

	public static LivingShopObjectTypes LIVING() {
		return getInstance().getLivingShopObjectTypes();
	}

	public static SignShopObjectType<?> SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static CitizensShopObjectType<?> CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
