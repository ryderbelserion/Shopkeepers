package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingEntityObjectTypes;
import com.nisovin.shopkeepers.shopobjects.sign.SignShopObjectType;

public class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	private final SKLivingEntityObjectTypes livingEntityObjectTypes = new SKLivingEntityObjectTypes();
	private final SignShopObjectType signShopObjectType = new SignShopObjectType();
	private final CitizensShopObjectType citizensShopObjectType = new CitizensShopObjectType();

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	public SKDefaultShopObjectTypes() {
	}

	@Override
	public List<AbstractShopObjectType<?>> getAllObjectTypes() {
		List<AbstractShopObjectType<?>> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(livingEntityObjectTypes.getAllObjectTypes());
		shopObjectTypes.add(signShopObjectType);
		shopObjectTypes.add(citizensShopObjectType);
		return shopObjectTypes;
	}

	@Override
	public SKLivingEntityObjectTypes getLivingEntityObjectTypes() {
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

	public static SKLivingEntityObjectTypes MOBS() {
		return getInstance().getLivingEntityObjectTypes();
	}

	public static SignShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static CitizensShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
