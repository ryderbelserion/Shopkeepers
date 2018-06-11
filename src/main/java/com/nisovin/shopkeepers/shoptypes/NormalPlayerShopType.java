package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class NormalPlayerShopType extends AbstractPlayerShopType<NormalPlayerShopkeeper> {

	NormalPlayerShopType() {
		super("player", ShopkeepersPlugin.PLAYER_NORMAL_PERMISSION);
	}

	@Override
	public NormalPlayerShopkeeper createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.validateCreationData(shopCreationData);
		NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper(id, (PlayerShopCreationData) shopCreationData);
		return shopkeeper;
	}

	@Override
	public NormalPlayerShopkeeper loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.validateConfigSection(configSection);
		NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper(id, configSection);
		return shopkeeper;
	}

	@Override
	protected String getCreatedMessage() {
		return Settings.msgPlayerShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("norm") || identifier.startsWith("sell");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedNormalShop);
	}
}
