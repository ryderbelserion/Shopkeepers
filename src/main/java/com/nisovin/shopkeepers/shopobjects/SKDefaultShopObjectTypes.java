package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.AbstractShopObjectType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectTypes;

public class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	private final LivingEntityObjectTypes livingEntityObjectTypes = new LivingEntityObjectTypes();
	private final AbstractShopObjectType signShopObjectType = new SignShopObjectType();
	private final AbstractShopObjectType citizensShopObjectType = new CitizensShopObjectType();

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	public SKDefaultShopObjectTypes() {
	}

	public void register() {
		ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().registerAll(this.getAllObjectTypes());
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
	public AbstractShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	@Override
	public AbstractShopObjectType getCitizensShopObjectType() {
		return citizensShopObjectType;
	}

	// STATICS (for convenience):

	public static SKDefaultShopObjectTypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultShopObjectTypes();
	}

	public static LivingEntityObjectTypes MOBS() {
		return getInstance().getLivingEntityObjectTypes();
	}

	public static AbstractShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static AbstractShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
