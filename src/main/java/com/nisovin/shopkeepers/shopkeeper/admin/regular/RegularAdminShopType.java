package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopType;

public class RegularAdminShopType extends AbstractAdminShopType<RegularAdminShopkeeper> {

	public RegularAdminShopType() {
		super("admin", ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Settings.msgShopTypeAdminRegular;
	}

	@Override
	public String getDescription() {
		return Settings.msgShopTypeDescAdminRegular;
	}

	@Override
	public String getSetupDescription() {
		return Settings.msgShopSetupDescAdminRegular;
	}

	@Override
	public RegularAdminShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		RegularAdminShopkeeper shopkeeper = new RegularAdminShopkeeper(id, shopCreationData);
		return shopkeeper;
	}

	@Override
	public RegularAdminShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		RegularAdminShopkeeper shopkeeper = new RegularAdminShopkeeper(id, configSection);
		return shopkeeper;
	}
}
