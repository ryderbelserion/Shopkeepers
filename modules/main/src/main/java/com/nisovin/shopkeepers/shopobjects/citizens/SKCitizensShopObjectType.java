package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;

public class SKCitizensShopObjectType extends AbstractEntityShopObjectType<SKCitizensShopObject> implements CitizensShopObjectType<SKCitizensShopObject> {

	private final CitizensShops citizensShops;

	public SKCitizensShopObjectType(CitizensShops citizensShops) {
		super("citizen", Arrays.asList("npc"), "shopkeeper.citizen");
		this.citizensShops = citizensShops;
	}

	@Override
	public boolean isEnabled() {
		return citizensShops.isEnabled();
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
	public Object getObjectId(Entity entity) {
		// We use the NPC's unique id as identifier:
		// Note: On later versions of Citizens this matches the entity UUID of the NPC entity (actually the other way
		// around: Citizens injects the NPC UUID into the entity). However, since we expect that each entity represents
		// at most one shopkeeper, this is not an issue in regards to potential conflicts with the object ids of other
		// shopkeepers and object types.
		UUID npcUniqueId = citizensShops.getNPCUniqueId(entity); // Null if the entity is not a NPC
		if (npcUniqueId == null) return null; // Entity is not a NPC
		return this.getObjectId(npcUniqueId);
	}

	Object getObjectId(UUID npcUniqueId) {
		assert npcUniqueId != null;
		return npcUniqueId;
	}

	@Override
	public boolean mustBeSpawned() {
		return false; // Spawning and despawning is handled by Citizens.
	}

	@Override
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace) {
		// A reduced set of checks, compared to the default:
		if (spawnLocation == null || spawnLocation.getWorld() == null) return false;
		return true;
	}

	@Override
	public SKCitizensShopObject createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		return new SKCitizensShopObject(citizensShops, shopkeeper, creationData);
	}
}
