package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;

public class SKCitizensShopObjectType extends AbstractEntityShopObjectType<SKCitizensShopObject> implements CitizensShopObjectType<SKCitizensShopObject> {

	private final CitizensShops citizensShops;

	public SKCitizensShopObjectType(CitizensShops citizensShops) {
		super("citizen", Arrays.asList("npc"), "shopkeeper.citizen");
		this.citizensShops = citizensShops;
	}

	@Override
	public String getDisplayName() {
		return Messages.shopObjectTypeNpc;
	}

	@Override
	public boolean usesDefaultObjectIds() {
		// Uses NPC rather than entity specific ids.
		return false;
	}

	@Override
	public String createObjectId(Entity entity) {
		if (entity == null) return null;
		UUID npcUniqueId = citizensShops.getNPCUniqueId(entity);
		if (npcUniqueId == null) return null;
		return this.createObjectId(npcUniqueId);
	}

	public String createObjectId(UUID npcUniqueId) {
		return this.getIdentifier() + ":" + npcUniqueId;
	}

	// TODO Remove again at some point.
	public String createObjectId(int npcLegacyId) {
		return this.getIdentifier() + ":" + npcLegacyId;
	}

	@Override
	public SKCitizensShopObject createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new SKCitizensShopObject(citizensShops, shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return citizensShops.isEnabled();
	}

	@Override
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace) {
		// A reduced set of checks, compared to the default:
		if (spawnLocation == null || spawnLocation.getWorld() == null) return false;
		return true;
	}
}
