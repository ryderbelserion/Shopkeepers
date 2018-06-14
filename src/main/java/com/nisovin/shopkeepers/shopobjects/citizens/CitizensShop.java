package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

/**
 * Note: This relies on the regular living entity shopkeeper interaction handling.
 * TODO separate this?
 */
public class CitizensShop extends AbstractShopObject {

	public static String CREATION_DATA_NPC_ID_KEY = "CitizensNpcId";

	public static String getId(int npcId) {
		return "NPC-" + npcId;
	}

	private Integer npcId = null;
	// used by citizen shopkeeper traits: if false, this will not remove the npc on deletion:
	private boolean destroyNPC = true;

	protected CitizensShop(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		if (creationData != null) {
			// can be null here, as currently only NPC shopkeepers created by the shopkeeper trait provide the npc id
			// via the creation data:
			this.npcId = creationData.getValue(CREATION_DATA_NPC_ID_KEY);
		}
	}

	@Override
	public CitizensShopObjectType getObjectType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	// can be null if not set yet
	public Integer getNpcId() {
		return npcId;
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		if (configSection.isInt("npcId")) {
			npcId = configSection.getInt("npcId");
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		if (npcId != null) {
			configSection.set("npcId", npcId);
		}
	}

	@Override
	public void setup() {
		super.setup();
		if (npcId != null || !CitizensHandler.isEnabled()) return;

		// create npc:
		Log.debug("Creating citizens NPC for shopkeeper " + shopkeeper.getId());

		EntityType entityType;
		String name;
		if (shopkeeper instanceof PlayerShopkeeper) {
			// player shops will use a player npc:
			entityType = EntityType.PLAYER;
			name = ((PlayerShopkeeper) shopkeeper).getOwnerName();
		} else {
			entityType = EntityType.VILLAGER;
			name = "Shopkeeper";
		}

		// create npc:
		Location spawnLocation = this.getSpawnLocation();
		npcId = CitizensHandler.createNPC(spawnLocation, entityType, name);
		shopkeeper.markDirty();
	}

	// LIFE CYCLE

	protected void onTraitRemoval() {
		destroyNPC = false;
	}

	@Override
	public void delete() {
		if (destroyNPC) {
			NPC npc = this.getNPC();
			if (npc != null) {
				if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
					// let the trait handle npc related cleanup:
					npc.getTrait(CitizensShopkeeperTrait.class).onShopkeeperRemove();
				} else {
					Log.debug("Removing citizens NPC " + npc.getId() + " due to deletion of shopkeeper " + shopkeeper.getId());
					npc.destroy(); // the npc was created by us, so we remove it again
				}
			} else {
				// TODO: npc not yet loaded or citizens not enabled: how to remove the citizens npc later?
				// usually not a problem, because players cannot delete citizens shopkeepers if the corresponding
				// citizens npc isn't present in the world (exception: deletion via commands..)
			}
		}
		npcId = null;
		shopkeeper.markDirty();
	}

	// ACTIVATION

	public NPC getNPC() {
		if (npcId == null) return null;
		if (!CitizensHandler.isEnabled()) return null;
		return CitizensAPI.getNPCRegistry().getById(npcId);
	}

	public Entity getEntity() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getEntity();
	}

	@Override
	public boolean isActive() {
		return (this.getNPC() != null);
	}

	@Override
	public String getId() {
		return (npcId == null ? null : getId(npcId));
	}

	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		return new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + 0.5D, shopkeeper.getZ() + 0.5D);
	}

	@Override
	public boolean spawn() {
		return false; // handled by citizens
	}

	@Override
	public void despawn() {
		// handled by citizens
	}

	@Override
	public Location getLocation() {
		Entity entity = this.getEntity();
		return entity != null ? entity.getLocation() : null;
	}

	@Override
	public boolean check() {
		NPC npc = this.getNPC();
		if (npc == null) {
			// Not going to force Citizens creation, this seems like it could go really wrong.
		} else {
			Location currentLocation = npc.getStoredLocation();
			Location expectedLocation = this.getSpawnLocation();
			if (currentLocation == null) {
				npc.teleport(expectedLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
				Log.debug("Shopkeeper NPC (" + shopkeeper.getPositionString() + ") had no location, teleported");
			} else if (!currentLocation.getWorld().equals(expectedLocation.getWorld()) || currentLocation.distanceSquared(expectedLocation) > 1.0D) {
				shopkeeper.setLocation(currentLocation);
				Log.debug("Shopkeeper NPC (" + shopkeeper.getPositionString() + ") out of place, re-indexing");
			}
		}
		return false;
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return 32; // TODO citizens seem to have different limits depending on mob type (16 for mobs, 64 for players)
	}

	@Override
	public void setName(String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.prepareName(name);
			// set entity name plate:
			npc.setName(name);
			// this.entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			npc.setName("");
			// this.entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getName();
	}

	// SUB TYPES

	// TODO: Support sub types? A menu of entity types here would be cool

	// OTHER PROPERTIES

	// TODO: Support equipping items? Is there a generic Citizens API for this?
}
