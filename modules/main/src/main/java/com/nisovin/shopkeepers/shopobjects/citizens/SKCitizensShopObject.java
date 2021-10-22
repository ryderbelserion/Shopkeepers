package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;

/**
 * Note: This relies on the regular living entity shopkeeper interaction handling.
 * TODO separate this?
 */
public class SKCitizensShopObject extends AbstractEntityShopObject implements CitizensShopObject {

	// Null if there is no associated NPC, eg. because no NPC has been created yet for the shop object (eg. if Citizens
	// was not enabled at the time the shop object has been created):
	public static final Property<UUID> NPC_UNIQUE_ID = new BasicProperty<UUID>()
			.dataKeyAccessor("npcId", UUIDSerializers.LENIENT)
			.nullable()
			.defaultValue(null)
			.build();
	public static final String CREATION_DATA_NPC_UUID_KEY = "CitizensNpcUUID";
	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_PERIOD_SECONDS + 1);

	protected final CitizensShops citizensShops;

	private final PropertyValue<UUID> npcUniqueIdProperty = new PropertyValue<>(NPC_UNIQUE_ID)
			.onValueChanged((property, oldValue, newValue, updateFlags) -> this.onNPCUniqueIdChanged(oldValue, newValue))
			.build(properties);

	// Only used initially, when the shopkeeper is created by a player. If this name is not available when we create the
	// NPC, we fall back to a different name.
	private String creatorName = null;
	// If false, this will not remove the NPC on deletion:
	private boolean destroyNPC = true;

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkLimiter = new RateLimiter(CHECK_PERIOD_SECONDS, nextCheckingOffset.getAndIncrement());

	protected SKCitizensShopObject(CitizensShops citizensShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.citizensShops = citizensShops;
		if (creationData != null) {
			// Can be null here, as currently only NPC shopkeepers created by the shopkeeper trait provide the NPC's
			// unique id via the creation data:
			UUID npcId = creationData.getValue(CREATION_DATA_NPC_UUID_KEY);
			npcUniqueIdProperty.setValue(npcId, Collections.emptySet()); // Not marking dirty
			Player creator = creationData.getCreator();
			this.creatorName = (creator != null) ? creator.getName() : null;
		}
	}

	@Override
	public SKCitizensShopObjectType getType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		npcUniqueIdProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		npcUniqueIdProperty.save(shopObjectData);
	}

	// NPC ID

	// Can be null if not yet set.
	public UUID getNPCUniqueId() {
		return npcUniqueIdProperty.getValue();
	}

	private void setNPCUniqueId(UUID npcId) {
		npcUniqueIdProperty.setValue(npcId); // Can be null
	}

	private void onNPCUniqueIdChanged(UUID oldValue, UUID newValue) {
		// Update registrations:
		if (shopkeeper.isValid()) {
			if (oldValue != null) {
				citizensShops.unregisterCitizensShopkeeper(SKCitizensShopObject.this, oldValue);
			}
			if (newValue != null) {
				citizensShops.registerCitizensShopkeeper(SKCitizensShopObject.this, newValue);
			}
			SKCitizensShopObject.this.onIdChanged();
		}
	}

	// NPC

	private void createNpcIfMissing() {
		if (this.getNPCUniqueId() != null || !citizensShops.isEnabled()) return;

		Log.debug(() -> shopkeeper.getLogPrefix() + "Creating Citizens NPC.");

		EntityType entityType = Settings.defaultCitizenNpcType;
		// Note: The spawn location can be null if the world is not loaded currently. We can still create the NPC, but
		// spawning will happen later once the world is loaded.
		Location spawnLocation = this.getSpawnLocation(); // Can be null
		// The NPC name is initially empty (works fine, even for player NPCs):
		NPC npc = citizensShops.createNPC(spawnLocation, entityType, "");
		if (npc == null) return; // NPC creation failed for some reason

		this.setNPCUniqueId(npc.getUniqueId());

		// Empty initial name for non-player NPCs:
		String name = "";
		if (entityType == EntityType.PLAYER) {
			// The name influences the initial skin of the player NPC:
			if (shopkeeper instanceof PlayerShopkeeper) {
				// Use the owner name for player shops (regardless of who actually created the shop):
				name = ((PlayerShopkeeper) shopkeeper).getOwnerName();
			} else {
				// Fallback to empty name (works fine, even for player NPCs):
				name = (creatorName != null) ? creatorName : "";
			}
		}
		assert name != null;

		// Apply the name:
		// This also applies the nameplate prefix and adjusts the nameplate visibility if required.
		// This also triggers a save of the NPC data if required.
		this.setName(name);
	}

	public NPC getNPC() {
		UUID npcUniqueId = this.getNPCUniqueId();
		if (npcUniqueId == null) return null;
		if (!citizensShops.isEnabled()) return null;
		return CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId);
	}

	private EntityType getEntityType() {
		NPC npc = this.getNPC();
		if (npc == null) return null;

		Entity entity = npc.getEntity(); // Null if not spawned
		if (entity != null) return entity.getType();

		return npc.getOrAddTrait(MobType.class).getType();
	}

	// LIFE CYCLE

	protected void setKeepNPCOnDeletion() {
		destroyNPC = false;
	}

	@Override
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onShopkeeperAdded(cause);

		// Create the NPC if required (and possible):
		this.createNpcIfMissing();

		// Register:
		citizensShops.registerCitizensShopkeeper(this, this.getNPCUniqueId());
	}

	@Override
	public void remove() {
		super.remove();

		// Unregister:
		citizensShops.unregisterCitizensShopkeeper(this, this.getNPCUniqueId());
	}

	@Override
	public void delete() {
		super.delete();
		// Check if there even is a corresponding NPC (maybe it has already been deleted, or it has not actually been
		// created yet):
		if (this.getNPCUniqueId() == null) return;
		if (destroyNPC) {
			NPC npc = this.getNPC();
			if (npc != null) {
				CitizensShopkeeperTrait shopkeeperTrait = npc.getTraitNullable(CitizensShopkeeperTrait.class);
				if (shopkeeperTrait != null) {
					// Let the trait handle NPC related cleanup (i.e. we only remove the trait in this case):
					shopkeeperTrait.onShopkeeperDeletion(shopkeeper);
				} else {
					Log.debug(() -> shopkeeper.getUniqueIdLogPrefix() + "Deleting Citizens NPC "
							+ CitizensShops.getNPCIdString(npc) + " due to shopkeeper deletion.");
					npc.destroy(); // The NPC was created by us, so we remove it again
					citizensShops.onNPCEdited(npc);
				}
			} else {
				// TODO: NPC not yet loaded or citizens not enabled: How to remove the citizens npc later?
				// Usually not a problem, because players cannot delete citizens shopkeepers if the corresponding
				// citizens NPC isn't present in the world (exception: deletion via commands..).
			}
		}
		this.setNPCUniqueId(null);
	}

	/**
	 * Called when the corresponding Citizens NPC is about to be deleted.
	 * 
	 * @param player
	 *            the player who deleted the NPC, can be <code>null</code> if not available
	 */
	void onNPCDeleted(Player player) {
		NPC npc = this.getNPC();
		assert npc != null;
		Log.debug(() -> shopkeeper.getUniqueIdLogPrefix() + "Deletion due to the deletion of Citizens NPC "
				+ CitizensShops.getNPCIdString(npc) + (player == null ? "" : " by player " + TextUtils.getPlayerString(player)));
		this.setKeepNPCOnDeletion(); // The NPC is already getting deleted, so we don't need to delete it.
		shopkeeper.delete(player);
	}

	// ACTIVATION

	@Override
	public Entity getEntity() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getEntity(); // Can be null if the NPC is not spawned currently
	}

	@Override
	public boolean isActive() {
		NPC npc = this.getNPC();
		if (npc == null) return false;
		// Note: Citizens despawns the entity on chunk unloads. isSpawned checks if the entity is still alive.
		assert !npc.isSpawned() || (npc.getEntity() != null); // The entity is not null if the NPC is spawned
		return npc.isSpawned();
	}

	@Override
	public Object getId() {
		UUID npcUniqueId = this.getNPCUniqueId();
		if (npcUniqueId == null) return null;
		return this.getType().getObjectId(npcUniqueId);
	}

	private Location getSpawnLocation() {
		Location spawnLocation = shopkeeper.getLocation();
		if (spawnLocation == null) return null; // World not loaded currently
		spawnLocation.add(0.5D, 0.5D, 0.5D); // Spawn at the center of the block
		return spawnLocation;
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
		return (entity != null) ? entity.getLocation() : null;
	}

	// TICKING

	@Override
	public void tick() {
		super.tick();
		if (!checkLimiter.request()) {
			return;
		}

		NPC npc = this.getNPC();
		if (npc == null) {
			// Not going to force NPC creation, as this seems like it could go really wrong.
			return;
		}

		Location expectedLocation = this.getSpawnLocation();
		if (expectedLocation == null) {
			// The spawn location's world is not loaded currently.
			// We only tick shop objects in loaded chunks, but the world might have been unloaded during the ticking.
			return;
		}
		assert expectedLocation.getWorld() != null;

		// Indicate ticking activity for visualization:
		this.indicateTickActivity();

		// Check if the NPC has been spawned at least once before:
		Location currentLocation = npc.getStoredLocation();
		if (currentLocation == null) {
			assert !npc.isSpawned();
			Log.debug(() -> shopkeeper.getLocatedLogPrefix() + "Citizens NPC has no stored location. Attempting spawn.");
			// This will log a debug message from Citizens if it cannot spawn the NPC currently, but will then later
			// attempt to spawn it when the chunk gets loaded:
			npc.spawn(expectedLocation);
			// Note: We don't trigger a save of the Citizens NPC data if we only spawned the NPC / changed its stored
			// location. Even if this data is not eventually saved by Citizens, we will be able to just spawn the NPC
			// again.
			return;
		}
		// Citizens returns a null location instead of a location with null world:
		assert currentLocation != null && currentLocation.getWorld() != null;

		// Check if a previously spawned NPC entity is still there:
		// Note: If the entity is null the NPC might also have been intentionally despawned.
		Entity entity = npc.getEntity();
		if (entity != null && entity.isDead()) {
			// An entity has previously been spawned but is marked for removal (maybe some other plugin removed it). The
			// Citizens plugin doesn't automatically respawn the entity in this case, but we do (similar to non-Citizens
			// shopkeepers).
			// Note: Entity#isDead is sufficient to detect the entity removal. Checking the slightly more costly
			// Entity#isValid is not required, since Citizens removes the entity on chunk unloads anyways.
			Log.debug(() -> shopkeeper.getLocatedLogPrefix() + "Citizens NPC is missing. Attempting respawn.");
			// Note: We respawn the entity at its last known location, rather then the (expected) shopkeeper location.
			// This will log a debug message from Citizens if it cannot spawn the NPC currently:
			npc.spawn(currentLocation);
		} // Continue to update the shopkeeper location if the NPC moved since we last checked.

		// Update the shopkeeper's location if the NPC is able to move:
		if (!expectedLocation.getWorld().equals(currentLocation.getWorld()) || expectedLocation.distanceSquared(currentLocation) > 1.0D) {
			Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLocatedLogPrefix()
					+ "Citizens NPC moved. Updating shopkeeper location.");
			shopkeeper.setLocation(currentLocation);
		}
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		// Citizens uses different limits depending on the NPC type (64 for mobs, 46 for players).
		// Citizens might dynamically truncate the name of the corresponding NPC, and will then print a warning.
		if (this.getEntityType() == EntityType.PLAYER) return 46;
		return 64;
	}

	@Override
	public void setName(String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			boolean isPlayerNPC = (this.getEntityType() == EntityType.PLAYER);
			if (!isPlayerNPC) {
				name = Messages.nameplatePrefix + name;
			} // Else: The name (including the prefix) influence the NPC's skin, so we avoid adding the prefix.
			name = this.prepareName(name);

			// Set name:
			npc.setName(name);

			// Update the nameplate visibility:
			// Player NPC don't support the hover option (the nameplate is always shown regardless). Also, when using
			// the hover option on player NPCs, the nameplate is limited to a length of 16. Whereas otherwise, depending
			// on Citizens settings, player NPC either use scoreboards or holograms to display the NPC name, which are
			// not affected by this length limitation.
			npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, Settings.alwaysShowNameplates || isPlayerNPC ? "true" : "hover");
		} else {
			// Remove the name:
			npc.setName("");

			// Update the nameplate visibility:
			npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, "false");
		}

		citizensShops.onNPCEdited(npc);
	}

	@Override
	public String getName() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getName();
	}

	// PLAYER SHOP OWNER

	@Override
	public void onShopOwnerChanged() {
		if (!Settings.allowRenamingOfPlayerNpcShops) {
			// Update the NPC's name:
			String ownerName = ((PlayerShopkeeper) shopkeeper).getOwnerName();
			this.setName(ownerName);
		}
	}

	// EDITOR ACTIONS

	// TODO: Support sub types? A menu of entity types here would be cool.
	// TODO: Support equipping items? Is there a generic Citizens API for this?
}
