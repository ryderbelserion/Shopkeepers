package com.nisovin.shopkeepers.shopkeeper.player.sell;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class SellingPlayerShopType extends AbstractPlayerShopType<SellingPlayerShopkeeper> {

	public SellingPlayerShopType() {
		super("sell", ShopkeepersPlugin.PLAYER_SELL_PERMISSION);
	}

	@Override
	public SellingPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		SellingPlayerShopkeeper shopkeeper = new SellingPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public SellingPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		SellingPlayerShopkeeper shopkeeper = new SellingPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}

	@Override
	protected String getCreatedMessage() {
		return Settings.msgSellShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("norm") || identifier.startsWith("sell") || identifier.startsWith("player");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedSellShop);
	}
}
