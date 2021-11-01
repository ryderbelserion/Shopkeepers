package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopType;

public final class RegularAdminShopType extends AbstractAdminShopType<SKRegularAdminShopkeeper> {

	public RegularAdminShopType() {
		super("admin", Collections.emptyList(), ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeAdminRegular;
	}

	@Override
	public String getDescription() {
		return Messages.shopTypeDescAdminRegular;
	}

	@Override
	public String getSetupDescription() {
		return Messages.shopSetupDescAdminRegular;
	}

	@Override
	public List<String> getTradeSetupDescription() {
		return Messages.tradeSetupDescAdminRegular;
	}

	@Override
	public SKRegularAdminShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SKRegularAdminShopkeeper shopkeeper = new SKRegularAdminShopkeeper(id, shopCreationData);
		return shopkeeper;
	}

	@Override
	public SKRegularAdminShopkeeper loadShopkeeper(int id, ShopkeeperData shopkeeperData) throws ShopkeeperCreateException {
		this.validateShopkeeperData(shopkeeperData);
		SKRegularAdminShopkeeper shopkeeper = new SKRegularAdminShopkeeper(id, shopkeeperData);
		return shopkeeper;
	}
}
