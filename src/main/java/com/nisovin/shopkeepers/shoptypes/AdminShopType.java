package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.registry.ShopkeeperCreateException;

public class AdminShopType extends AbstractShopType<AdminShopkeeper> {

	AdminShopType() {
		super("admin", ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	public AdminShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		AdminShopkeeper shopkeeper = new AdminShopkeeper(id, shopCreationData);
		return shopkeeper;
	}

	@Override
	public AdminShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		AdminShopkeeper shopkeeper = new AdminShopkeeper(id, configSection);
		return shopkeeper;
	}

	@Override
	protected String getCreatedMessage() {
		return Settings.msgAdminShopCreated;
	}

	@Override
	protected void onSelect(Player player) {
		// currently can't be 'selected'
	}
}
