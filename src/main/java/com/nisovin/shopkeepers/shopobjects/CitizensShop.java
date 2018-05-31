package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopType;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

public class CitizensShop extends ShopObject {

	public static String CREATION_DATA_NPC_ID_KEY = "CitizensNpcId";

	public static String getId(int npcId) {
		return "NPC-" + npcId;
	}

	private Integer npcId = null;
	// used by citizen shopkeeper traits: if false, this will not remove the npc on deletion:
	private boolean destroyNPC = true;

	protected CitizensShop(Shopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		if (creationData != null) {
			// can be null here, as currently only NPC shopkeepers created by the shopkeeper trait provide the npc id
			// via the creation data:
			this.npcId = creationData.getValue(CREATION_DATA_NPC_ID_KEY);
		}
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		if (config.contains("npcId")) {
			npcId = config.getInt("npcId");
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		if (npcId != null) {
			config.set("npcId", npcId);
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.isActive()) return;
		if (!CitizensHandler.isEnabled()) return;

		// create npc:
		EntityType entityType;
		String name;
		if (shopkeeper.getType() instanceof PlayerShopType) {
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
	}

	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		return new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + 0.5D, shopkeeper.getZ() + 0.5D);
	}

	@Override
	public CitizensShopObjectType getObjectType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	@Override
	public boolean spawn() {
		return false; // handled by citizens
	}

	@Override
	public boolean isActive() {
		return npcId != null && CitizensHandler.isEnabled();
	}

	@Override
	public String getId() {
		return npcId == null ? null : getId(npcId);
	}

	// can be null if not set yet
	public Integer getNpcId() {
		return npcId;
	}

	public NPC getNPC() {
		if (!this.isActive()) return null;
		return CitizensAPI.getNPCRegistry().getById(npcId);
	}

	public Entity getEntity() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getEntity();
	}

	@Override
	public Location getActualLocation() {
		Entity entity = this.getEntity();
		return entity != null ? entity.getLocation() : null;
	}

	@Override
	public void setName(String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.trimToNameLength(name);
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
	public int getNameLengthLimit() {
		return 32; // TODO citizens seem to have different limits depending on mob type (16 for mobs, 64 for players)
	}

	@Override
	public void setItem(ItemStack item) {
		// TODO: No Citizens API for equipping items?
	}

	@Override
	public boolean check() {
		NPC npc = this.getNPC();
		if (npc != null) {
			Location currentLocation = npc.getStoredLocation();
			Location expectedLocation = this.getSpawnLocation();
			if (currentLocation == null) {
				npc.teleport(expectedLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
				Log.debug("Shopkeeper NPC (" + shopkeeper.getPositionString() + ") had no location, teleported");
			} else if (!currentLocation.getWorld().equals(expectedLocation.getWorld()) || currentLocation.distanceSquared(expectedLocation) > 1.0D) {
				shopkeeper.setLocation(currentLocation);
				Log.debug("Shopkeeper NPC (" + shopkeeper.getPositionString() + ") out of place, re-indexing");
			}
		} else {
			// Not going to force Citizens creation, this seems like it could go really wrong.
		}

		return false;
	}

	@Override
	public void despawn() {
		// handled by citizens
	}

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
					npc.destroy(); // the npc was created by us, so we remove it again
				}
			}
		}
		npcId = null;
	}

	@Override
	public ItemStack getSubTypeItem() {
		// TODO: A menu of entity types here would be cool
		return null;
	}

	@Override
	public void cycleSubType() {
	}
}
