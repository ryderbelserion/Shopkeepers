package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class SKCitizensShopObjectType extends AbstractEntityShopObjectType<SKCitizensShopObject> implements CitizensShopObjectType<SKCitizensShopObject> {

	private final CitizensShops citizensShops;

	public SKCitizensShopObjectType(CitizensShops citizensShops) {
		super("citizen", "shopkeeper.citizen");
		this.citizensShops = citizensShops;
	}

	@Override
	public String createObjectId(Entity entity) {
		if (entity == null) return null;
		Integer npcId = citizensShops.getNPCId(entity);
		if (npcId == null) return null;
		return this.createObjectId(npcId);
	}

	public String createObjectId(int npcId) {
		return this.getIdentifier() + ":" + npcId;
	}

	@Override
	public SKCitizensShopObject createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new SKCitizensShopObject(citizensShops, shopkeeper, creationData);
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
