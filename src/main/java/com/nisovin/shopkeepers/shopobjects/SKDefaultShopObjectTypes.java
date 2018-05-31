package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.AbstractShopObjectType;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectTypes;

public class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	private final LivingEntityObjectTypes livingEntityObjectTypes = new LivingEntityObjectTypes();
	private final SignShopObjectType signShopObjectType = new SignShopObjectType();
	private final CitizensShopObjectType citizensShopObjectType = new CitizensShopObjectType();

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	public SKDefaultShopObjectTypes() {
	}

	@Override
	public List<AbstractShopObjectType> getAllObjectTypes() {
		List<AbstractShopObjectType> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(livingEntityObjectTypes.getAllObjectTypes());
		shopObjectTypes.add(signShopObjectType);
		shopObjectTypes.add(citizensShopObjectType);
		return shopObjectTypes;
	}

	@Override
	public LivingEntityObjectTypes getLivingEntityObjectTypes() {
		return livingEntityObjectTypes;
	}

	@Override
	public SignShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	@Override
	public CitizensShopObjectType getCitizensShopObjectType() {
		return citizensShopObjectType;
	}

	// STATICS (for convenience):

	public static SKDefaultShopObjectTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopObjectTypes();
	}

	public static LivingEntityObjectTypes MOBS() {
		return getInstance().getLivingEntityObjectTypes();
	}

	public static SignShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static CitizensShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
