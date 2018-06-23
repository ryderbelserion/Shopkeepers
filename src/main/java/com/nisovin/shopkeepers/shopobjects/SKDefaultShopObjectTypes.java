package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.citizens.SKCitizensShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.sign.SKSignShopObjectType;

public class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	private final SKShopkeepersPlugin plugin;

	public SKDefaultShopObjectTypes(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public List<AbstractShopObjectType<?>> getAll() {
		List<AbstractShopObjectType<?>> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(this.getLivingShopObjectTypes().getAll());
		shopObjectTypes.add(this.getSignShopObjectType());
		shopObjectTypes.add(this.getCitizensShopObjectType());
		return shopObjectTypes;
	}

	@Override
	public SKLivingShopObjectTypes getLivingShopObjectTypes() {
		return plugin.getLivingShops().getLivingEntityObjectTypes();
	}

	@Override
	public SKSignShopObjectType getSignShopObjectType() {
		return plugin.getSignShops().getSignShopObjectType();
	}

	@Override
	public SKCitizensShopObjectType getCitizensShopObjectType() {
		return plugin.getCitizensShops().getCitizensShopObjectType();
	}

	// STATICS (for convenience):

	public static SKDefaultShopObjectTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopObjectTypes();
	}

	public static SKLivingShopObjectTypes LIVING() {
		return getInstance().getLivingShopObjectTypes();
	}

	public static SKSignShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static SKCitizensShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}
}
