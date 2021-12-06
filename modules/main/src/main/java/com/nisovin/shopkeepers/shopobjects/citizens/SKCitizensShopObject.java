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
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;

/**
 * Note: This relies on the regular living entity shopkeeper interaction handling.
 * TODO separate this?
 */
public class SKCitizensShopObject extends AbstractEntityShopObject implements CitizensShopObject {

	// Null if there is no associated NPC, e.g. because no NPC has been created yet for the shop object (e.g. if
	// Citizens was not enabled at the time the shop object has been created):
	public static final Property<UUID> NPC_UNIQUE_ID = new BasicProperty<UUID>()
			.dataKeyAccessor("npcId", UUIDSerializers.LENIENT)
			.nullable()
			.defaultValue(null)
			.build();
	public static final Property<DataContainer> NPC_DATA = new BasicProperty<DataContainer>()
			.dataKeyAccessor("npc-data", DataContainerSerializers.DEFAULT)
			.nullable() // Saving of NPC data is optional
			.defaultValue(null)
			.build();

	public static final String CREATION_DATA_NPC_UUID_KEY = "CitizensNpcUUID";
	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_PERIOD_SECONDS + 1);

	protected final CitizensShops citizensShops;

	private final PropertyValue<UUID> npcUniqueIdProperty = new PropertyValue<>(NPC_UNIQUE_ID)
			.onValueChanged((property, oldValue, newValue, updateFlags) -> this.onNPCUniqueIdChanged(oldValue, newValue))
			.build(properties);
	private DataContainer npcData = null;

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
		this.loadNpcData(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		npcUniqueIdProperty.save(shopObjectData);
		this.saveNpcData(shopObjectData, saveAll);
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

	// Returns null if the NPC is not available and could not be created.
	private NPC getOrCreateNpcIfMissing() {
		NPC npc = this.getNPC();
		if (npc != null) return npc;
		if (this.getNPCUniqueId() != null) {
			// The NPC has already been created in the past. Even if it no longer exists, we won't force a recreation of
			// it, since periodically recreating new NPCs could go really wrong.
			return null;
		}
		if (!citizensShops.isEnabled()) return null;

		Log.debug(() -> shopkeeper.getLogPrefix() + "Creating Citizens NPC.");

		EntityType entityType = Settings.defaultCitizenNpcType;
		// Note: The spawn location can be null if the world is not loaded currently. We can still create the NPC, but
		// spawning will happen later once the world is loaded.
		Location spawnLocation = this.getSpawnLocation(); // Can be null
		// The NPC name is initially empty (works fine, even for player NPCs):
		npc = citizensShops.createNPC(spawnLocation, entityType, "");
		if (npc == null) {
			// NPC creation failed for some reason.
			Log.debug(() -> shopkeeper.getLogPrefix() + "Failed to create Citizens NPC!");
			return null;
		}

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

		return npc;
	}

	private void setupNpc() {
		NPC npc = this.getOrCreateNpcIfMissing();
		if (npc == null) return; // NPC is not available

		this.applyNpcData();
		this.updateNpcOwner();
	}

	private void updateNpcOwner() {
		if (!(shopkeeper instanceof PlayerShopkeeper)) return;
		PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;

		NPC npc = this.getNPC();
		if (npc == null) return;

		boolean npcChanged = false;
		if (Settings.setCitizenNpcOwnerOfPlayerShops) {
			UUID ownerId = playerShop.getOwnerUUID();
			Owner ownerTrait = npc.getOrAddTrait(Owner.class);
			if (!ownerId.equals(ownerTrait.getOwnerId())) {
				ownerTrait.setOwner(ownerId);
				npcChanged = true;
				Log.debug(() -> shopkeeper.getLogPrefix() + "Citizens NPC owner set.");
			}
		} else if (npc.hasTrait(Owner.class)) {
			npc.removeTrait(Owner.class);
			npcChanged = true;
			Log.debug(() -> shopkeeper.getLogPrefix() + "Citizens NPC owner removed.");
		}

		if (npcChanged) {
			citizensShops.onNPCEdited(npc);
		}
	}

	// NPC DATA

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration("citizens-npc-data-cleanup",
				MigrationPhase.ofShopObjectClass(SKCitizensShopObject.class)) {
			@Override
			public boolean migrate(ShopkeeperData shopkeeperData, String logPrefix) throws InvalidDataException {
				// When this setting has been disabled, we automatically delete any previously saved but no longer
				// required NPC data:
				if (Settings.snapshotsSaveCitizenNpcData) return false;

				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				DataContainer npcData = shopObjectData.get(NPC_DATA);
				if (npcData != null) {
					Log.warning(logPrefix + "Deleted previously saved Citizens NPC data!");
					shopObjectData.set(NPC_DATA, null);
					return true;
				}
				return false;
			}
		});
	}

	private void loadNpcData(ShopObjectData shopObjectData) throws InvalidDataException {
		// Note: If we store previously restored but not yet applied NPC data, and we then restore another snapshot that
		// does not store any NPC data, we would normally clear the currently stored but not yet applied NPC data! In
		// order to prevent this loss of data, we keep the previous NPC data in this case.
		// TODO However, there might be situations in which we want the shop object to strictly adapt the loaded state.
		// Ideally, this would require some kind of flag to differentiate here between a snapshot being applied vs the
		// shop object being loaded normally. However, this isn't much of an issue currently: When we normally reload
		// the shopkeepers from disk (eg. during a reload of the plugin), we fully recreate the shop objects. So any
		// state in memory is lost anyways (if it hasn't been saved to disk before).
		DataContainer previousNpcData = this.npcData;
		this.npcData = shopObjectData.get(NPC_DATA);
		if (npcData == null && previousNpcData != null) {
			Log.warning(shopkeeper.getLogPrefix()
					+ "Prevented previously restored but not yet applied Citizens NPC data from being cleared!");
			this.npcData = previousNpcData;
			shopkeeper.markDirty(); // Our shop object state differs from the loaded data
		}
		if (npcData != null) {
			// TODO Do we need to somehow apply NPC data migrations?
			// Previously saved but no longer needed NPC data is deleted during data migrations:
			assert Settings.snapshotsSaveCitizenNpcData;
			// Update the NPC, if it is currently available:
			if (shopkeeper.isValid()) {
				this.setupNpc();
			}
			// Else: The shopkeeper is currently being set up. The NPC setup is triggered once the shopkeeper is added
			// to the shopkeeper registry.
		}
	}

	private void saveNpcData(ShopObjectData shopObjectData, boolean saveAll) {
		// If the saving of NPC data is disabled, we also skip the saving of any previously restored but not yet applied
		// NPC data.
		if (!Settings.snapshotsSaveCitizenNpcData) return;

		// There is no need to try to get the current NPC data if we still store previously restored NPC data that we
		// were not yet able to apply (because the NPC hasn't been loaded in the meantime).
		DataContainer npcData = this.npcData;
		if (saveAll && npcData == null) {
			NPC npc = this.getNPC();
			UUID npcUniqueId = this.getNPCUniqueId();
			if (npc == null && npcUniqueId != null) {
				Log.warning(shopkeeper.getLogPrefix()
						+ "Citizens NPC not found! Is Citizens running? Could not save data of Citizens NPC with uuid "
						+ npcUniqueId);
				return;
			}
			npcData = CitizensUtils.saveNpc(npc);
		}
		shopObjectData.set(NPC_DATA, npcData);
	}

	// It may be necessary to also apply other changes to the NPC after the NPC state has been restored. Usually, this
	// should therefore not be called directly, but as part of #setupNpc().
	private void applyNpcData() {
		if (this.npcData == null) return; // Nothing to apply

		NPC npc = this.getNPC();
		if (npc == null) {
			// The NPC is not available currently. We remember the NPC data and try to apply it once the NPC becomes
			// available again.
			return;
		}

		Log.debug(() -> shopkeeper.getLogPrefix() + "Applying stored Citizens NPC state to NPC " + npc.getId());
		CitizensUtils.loadNpc(npc, npcData);

		citizensShops.onNPCEdited(npc);

		// Once applied, we can delete our copy of the NPC data:
		this.npcData = null;
		shopkeeper.markDirty();
	}

	// LIFE CYCLE

	protected void setKeepNPCOnDeletion() {
		destroyNPC = false;
	}

	@Override
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onShopkeeperAdded(cause);

		// Set up the NPC:
		// If not yet done, this will first try to create the NPC.
		this.setupNpc();

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

	/**
	 * This is called after Citizens shops have been enabled.
	 * <p>
	 * This might be called a few ticks after the actual enabling of Citizens shops, and only if the Citizens shops are
	 * still enabled at this point.
	 */
	void onCitizensShopsEnabled() {
		// Setup the NPC:
		this.setupNpc();
	}

	/**
	 * This is called when Citizens shops are being disabled.
	 */
	void onCitizensShopsDisabled() {
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
			// Note: We respawn the entity at its last known location, rather than the (expected) shopkeeper location.
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
		super.onShopOwnerChanged();
		assert shopkeeper instanceof PlayerShopkeeper;

		this.updateNpcOwner();

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
