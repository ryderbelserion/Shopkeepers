package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class CitizensShopObjectType extends AbstractShopObjectType<CitizensShop> {

	private final CitizensShops citizensShops;

	public CitizensShopObjectType(CitizensShops citizensShops) {
		super("citizen", "shopkeeper.citizen");
		this.citizensShops = citizensShops;
	}

	@Override
	public CitizensShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new CitizensShop(citizensShops, shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableCitizenShops && citizensShops.isEnabled();
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("citizen") || identifier.startsWith("npc");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedCitizenShop);
	}

	@Override
	public boolean needsSpawning() {
		return false; // spawning and despawning is handled by citizens
	}
}
