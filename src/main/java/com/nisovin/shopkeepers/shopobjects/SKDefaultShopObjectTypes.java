package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingEntityObjectTypes;
import com.nisovin.shopkeepers.shopobjects.sign.SignShopObjectType;

public class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	private final SKShopkeepersPlugin plugin;

	public SKDefaultShopObjectTypes(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public List<AbstractShopObjectType<?>> getAllObjectTypes() {
		List<AbstractShopObjectType<?>> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(this.getLivingEntityObjectTypes().getAllObjectTypes());
		shopObjectTypes.add(this.getSignShopObjectType());
		shopObjectTypes.add(this.getCitizensShopObjectType());
		return shopObjectTypes;
	}

	@Override
	public SKLivingEntityObjectTypes getLivingEntityObjectTypes() {
		return plugin.getLivingEntityShops().getLivingEntityObjectTypes();
	}

	@Override
	public SignShopObjectType getSignShopObjectType() {
		return plugin.getSignShops().getSignShopObjectType();
	}

	@Override
	public CitizensShopObjectType getCitizensShopObjectType() {
		return plugin.getCitizensShops().getCitizensShopObjectType();
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
