package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;

/**
 * Note: This relies on the regular living entity shopkeeper interaction handling.
 * TODO separate this?
 */
public class SKCitizensShopObject extends AbstractEntityShopObject implements CitizensShopObject {

	public static String CREATION_DATA_NPC_UUID_KEY = "CitizensNpcUUID";

	protected final CitizensShops citizensShops;
	private UUID npcUniqueId = null;
	private Integer npcLegacyId = null;
	// If false, this will not remove the NPC on deletion:
	private boolean destroyNPC = true;

	protected SKCitizensShopObject(CitizensShops citizensShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.citizensShops = citizensShops;
		if (creationData != null) {
			// Can be null here, as currently only NPC shopkeepers created by the shopkeeper trait provide the NPC's
			// unique id via the creation data:
			this.npcUniqueId = creationData.getValue(CREATION_DATA_NPC_UUID_KEY);
		}
	}

	@Override
	public SKCitizensShopObjectType getType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	// Can be null if not set yet.
	public UUID getNPCUniqueId() {
		return npcUniqueId;
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		if (configSection.contains("npcId")) {
			// Legacy conversion from integer ids
			// TODO Remove again at some point.
			if (configSection.isInt("npcId")) {
				npcLegacyId = configSection.getInt("npcId");
			} else {
				String npcUniqueIdString = configSection.getString("npcId");
				this.npcUniqueId = ConversionUtils.parseUUID(npcUniqueIdString);
				if (npcUniqueId == null) {
					Log.warning("Couldn't parse NPC unique id for shopkeeper " + shopkeeper.getId() + ": " + npcUniqueIdString);
				}
			}
		}
	}

	// TODO Remove again at some point.
	// Gets called from CitizensShops.
	void convertLegacyId() {
		if (npcLegacyId != null && citizensShops.isEnabled()) {
			assert npcUniqueId == null;
			NPC npc = CitizensAPI.getNPCRegistry().getById(npcLegacyId);
			if (npc != null) {
				Log.info("Citizens shopkeeper id conversion: Mapping shopkeeper " + shopkeeper.getId() + " to NPC " + CitizensShops.getNPCIdString(npc));
				npcUniqueId = npc.getUniqueId();
				npcLegacyId = null;
				shopkeeper.markDirty();

				// Re-activate by new object id:
				SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperObjectIdChanged(shopkeeper);
			}
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		if (npcUniqueId != null) {
			configSection.set("npcId", npcUniqueId.toString());
		} else if (npcLegacyId != null) {
			// TODO Remove again at some point.
			configSection.set("npcId", npcLegacyId);
		}
	}

	@Override
	public void setup() {
		super.setup();
		if (npcLegacyId != null || npcUniqueId != null || !citizensShops.isEnabled()) return;

		// Create NPC:
		Log.debug(() -> "Creating citizens NPC for shopkeeper " + shopkeeper.getId());

		EntityType entityType;
		String name;
		if (shopkeeper instanceof PlayerShopkeeper) {
			// Player shops will use a player NPC:
			entityType = EntityType.PLAYER;
			name = ((PlayerShopkeeper) shopkeeper).getOwnerName();
		} else {
			entityType = EntityType.VILLAGER;
			name = "Shopkeeper";
		}

		// Create NPC:
		// Note: The spawn location can be null if the world is not loaded currently. We can still create the NPC, but
		// spawning will happen later once the world is loaded.
		Location spawnLocation = this.getSpawnLocation(); // Can be null
		npcUniqueId = citizensShops.createNPC(spawnLocation, entityType, name);
		shopkeeper.markDirty();
	}

	// LIFE CYCLE

	protected void setKeepNPCOnDeletion() {
		destroyNPC = false;
	}

	@Override
	public void delete() {
		if (npcUniqueId == null) return; // There is no corresponding NPC (maybe already deleted)
		if (destroyNPC) {
			NPC npc = this.getNPC();
			if (npc != null) {
				if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
					// Let the trait handle NPC related cleanup (i.e. we only remove the trait in this case):
					npc.getTrait(CitizensShopkeeperTrait.class).onShopkeeperDeletion(shopkeeper);
				} else {
					Log.debug(() -> "Removing Citizens NPC " + CitizensShops.getNPCIdString(npc)
							+ " due to deletion of shopkeeper " + shopkeeper.getIdString());
					npc.destroy(); // The NPC was created by us, so we remove it again
				}
			} else {
				// TODO: NPC not yet loaded or citizens not enabled: How to remove the citizens npc later?
				// Usually not a problem, because players cannot delete citizens shopkeepers if the corresponding
				// citizens NPC isn't present in the world (exception: deletion via commands..).
			}
		}
		npcUniqueId = null;
		shopkeeper.markDirty();
	}

	/**
	 * Called when the corresponding Citizens NPC is about to get deleted.
	 * 
	 * @param player
	 *            the player who deleted the NPC, can be <code>null</code> if not available
	 */
	void onNPCDeleted(Player player) {
		NPC npc = this.getNPC();
		assert npc != null;
		Log.debug(() -> "Removing shopkeeper " + shopkeeper.getIdString() + " due to the deletion of Citizens NPC "
				+ CitizensShops.getNPCIdString(npc) + (player == null ? "" : " by player " + TextUtils.getPlayerString(player)));
		this.setKeepNPCOnDeletion(); // The NPC is already getting deleted, so we don't need to delete it.
		shopkeeper.delete(player);
	}

	// ACTIVATION

	public NPC getNPC() {
		if (npcUniqueId == null) return null;
		if (!citizensShops.isEnabled()) return null;
		return CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId);
	}

	@Override
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
		if (npcUniqueId == null) {
			if (npcLegacyId == null) return null;
			else {
				return this.getType().createObjectId(npcLegacyId);
			}
		}
		return this.getType().createObjectId(npcUniqueId);
	}

	private Location getSpawnLocation() {
		assert shopkeeper.getWorldName() != null;
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world == null) return null; // World not loaded currently
		return new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + 0.5D, shopkeeper.getZ() + 0.5D);
	}

	@Override
	public boolean needsSpawning() {
		return false; // Spawning and despawning is handled by Citizens.
	}

	@Override
	public boolean spawn() {
		return false; // Handled by Citizens.
	}

	@Override
	public void despawn() {
		// Handled by Citizens.
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
			// Not going to force NPC creation, as this seems like it could go really wrong.
			return false;
		}

		Location expectedLocation = this.getSpawnLocation();
		if (expectedLocation == null) {
			// The spawn location's world is not loaded currently.
			return false;
		}
		assert expectedLocation.getWorld() != null;

		Location currentLocation = npc.getStoredLocation();
		if (currentLocation == null) {
			assert !npc.isSpawned();
			// This will log a debug message from Citizens if it cannot spawn the NPC currently, but will then later
			// attempt to spawn it when the chunk gets loaded:
			npc.spawn(expectedLocation);
			Log.debug(() -> "Shopkeeper NPC (" + shopkeeper.getPositionString() + ") had no location, attempted spawn");
			return false;
		}
		assert currentLocation.getWorld() != null; // Citizens will return a null Location in this case

		if (!expectedLocation.getWorld().equals(currentLocation.getWorld()) || expectedLocation.distanceSquared(currentLocation) > 1.0D) {
			shopkeeper.setLocation(currentLocation);
			Log.debug(() -> "Shopkeeper NPC (" + shopkeeper.getPositionString() + ") out of place, re-indexing");
			return false;
		}
		return false;
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		// Citizens uses different limits depending on the npc type (64 for mobs, 46 for players).
		// Citizens might dynamically truncate the name of the corresponding npc, and will then print a warning.
		NPC npc = this.getNPC();
		if (npc != null && npc.getTrait(MobType.class).getType() == EntityType.PLAYER) return 46;
		return 64;
	}

	@Override
	public void setName(String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			name = Messages.nameplatePrefix + name;
			name = this.prepareName(name);
			// Set entity name plate:
			npc.setName(name);
			// this.entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// Remove name plate:
			npc.setName("");
			// this.entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getFullName();
	}

	// EDITOR ACTIONS

	// TODO: Support sub types? A menu of entity types here would be cool.
	// TODO: Support equipping items? Is there a generic Citizens API for this?
}
