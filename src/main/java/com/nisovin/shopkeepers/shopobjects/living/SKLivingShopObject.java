package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.debug.events.DebugListener;
import com.nisovin.shopkeepers.debug.events.EventDebugListener;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.CyclicCounter;
import com.nisovin.shopkeepers.util.EntityUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.RateLimiter;
import com.nisovin.shopkeepers.util.Utils;

public class SKLivingShopObject<E extends LivingEntity> extends AbstractEntityShopObject implements LivingShopObject {

	/**
	 * We check from slightly below the top of the spawn block (= offset) in a range of up to one block below the spawn
	 * block (= range) for a location to spawn the shopkeeper entity at.
	 */
	protected static final double SPAWN_LOCATION_OFFSET = 0.98D;
	protected static final double SPAWN_LOCATION_RANGE = 2.0D;

	protected static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_PERIOD_SECONDS + 1);
	// If the entity could not be respawned this amount of times, we throttle its tick rate (i.e. the rate at which we
	// attempt to respawn it):
	protected static final int MAX_RESPAWN_ATTEMPTS = 5;
	protected static final int THROTTLED_CHECK_PERIOD_SECONDS = 60;

	protected final LivingShops livingShops;
	private final SKLivingShopObjectType<?> livingObjectType;
	private E entity;
	private Location lastSpawnLocation = null;
	private int respawnAttempts = 0;
	private boolean debuggingSpawn = false;
	private static long lastSpawnDebugging = 0; // Shared among all living shopkeepers to prevent spam

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final int checkingOffset = nextCheckingOffset.getAndIncrement();
	private final RateLimiter checkLimiter = new RateLimiter(CHECK_PERIOD_SECONDS, checkingOffset);

	protected SKLivingShopObject(	LivingShops livingShops, SKLivingShopObjectType<?> livingObjectType,
									AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.livingShops = livingShops;
		this.livingObjectType = livingObjectType;
	}

	@Override
	public SKLivingShopObjectType<?> getType() {
		return livingObjectType;
	}

	@Override
	public EntityType getEntityType() {
		return livingObjectType.getEntityType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		// Check for legacy uuid entry:
		if (configSection.contains("uuid")) {
			// Mark dirty to remove this entry with the next save:
			shopkeeper.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
	}

	// ACTIVATION

	@Override
	public E getEntity() {
		return entity;
	}

	// Shopkeepers might be located 1 block above passable (especially in the past) or non-full blocks. In order to not
	// have these shopkeepers hover but stand on the ground, we determine the exact spawn location their entity would
	// fall to within the range of up to 1 block below their spawn block.
	// This also applies with gravity disabled, and even if the block below their spawn block is air now: Passable
	// blocks like grass or non-full blocks like carpets or slabs might have been broken since the shopkeeper was
	// created. We still want to place the shopkeeper nicely on the ground in those cases.
	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world == null) return null; // World not loaded

		Location spawnLocation = new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + SPAWN_LOCATION_OFFSET, shopkeeper.getZ() + 0.5D);
		double distanceToGround = Utils.getCollisionDistanceToGround(spawnLocation, SPAWN_LOCATION_RANGE);
		if (distanceToGround == SPAWN_LOCATION_RANGE) {
			// No collision within the checked range: Remove the initial offset from the spawn location.
			distanceToGround = SPAWN_LOCATION_OFFSET;
		}
		// Adjust spawn location:
		spawnLocation.add(0.0D, -distanceToGround, 0.0D);
		return spawnLocation;
	}

	// Any preparation that needs to be done before spawning. Might only allow limited operations.
	protected void prepareEntity(E entity) {
		// Assign metadata for easy identification by other plugins:
		ShopkeeperMetadata.apply(entity);

		// Don't save the entity to the world data:
		entity.setPersistent(false);

		// Apply name (if it has/uses one):
		this.applyName(entity, shopkeeper.getName());

		// Clear equipment:
		// Doing this during entity preparation resolves some issue with the equipment not getting cleared (or at least
		// not getting cleared visually).
		EntityEquipment equipment = entity.getEquipment();
		equipment.clear();

		// We give entities which would usually burn in sunlight an indestructible item as helmet. This results in less
		// EntityCombustEvents that need to be processed.
		if (entity instanceof Zombie || entity instanceof Skeleton) {
			// Note: Phantoms also burn in sunlight, but setting an helmet has no effect for them.
			// Note: Buttons are small enough to not be visible inside the entity's head (even for their baby variants).
			equipment.setHelmet(new ItemStack(Material.STONE_BUTTON));
		}
	}

	// Any clean up that needs to happen for the entity. The entity might not be fully setup yet.
	protected void cleanUpEntity(E entity) {
		assert entity != null;
		// Disable AI:
		this.cleanupAI();

		// Remove metadata again:
		ShopkeeperMetadata.remove(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean spawn() {
		if (entity != null) {
			return true; // Already spawned
		}

		// Prepare spawn location:
		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) {
			return false; // World not loaded
		}
		World world = spawnLocation.getWorld();
		assert world != null;

		// Spawn entity:
		// TODO Check if the block is passable before spawning there?
		EntityType entityType = this.getEntityType();
		// Note: We expect this type of entity to be spawnable, and not result in an IllegalArgumentException.
		entity = (E) world.spawn(spawnLocation, entityType.getEntityClass(), (entity) -> {
			// Note: This callback is run after the entity has been prepared (this includes the creation of random
			// equipment and the random spawning of passengers) and right before the entity gets added to the world
			// (which triggers the corresponding CreatureSpawnEvent).

			// Debugging entity spawning:
			if (entity.isDead()) {
				Log.debug("Spawning shopkeeper entity is dead already!");
			}

			// Prepare entity, before it gets spawned:
			prepareEntity((E) entity);

			// Try to bypass entity-spawn blocking plugins (right before this specific entity is about to get spawned):
			livingShops.forceCreatureSpawn(spawnLocation, entityType);
		});
		assert entity != null;

		boolean success = this.isActive();
		if (success) {
			// Remember the spawn location:
			this.lastSpawnLocation = spawnLocation;

			// Further setup entity after it was successfully spawned:
			// Some entities randomly spawn with passengers:
			for (Entity passenger : entity.getPassengers()) {
				passenger.remove();
			}
			entity.eject(); // Some entities might automatically mount on nearby entities (like baby zombies on chicken)
			entity.setRemoveWhenFarAway(false);
			entity.setCanPickupItems(false);

			// Disable breeding:
			if (entity instanceof Ageable) {
				Ageable ageable = ((Ageable) entity);
				ageable.setAdult();
				ageable.setBreed(false);
				ageable.setAgeLock(true);
			}

			// Set the entity to an adult if we don't support its baby property yet:
			NMSManager.getProvider().setExclusiveAdult(entity);

			// Remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}

			// Overwrite AI:
			this.overwriteAI();
			// Register the entity for our custom AI processing:
			livingShops.getLivingEntityAI().addEntity(entity);

			// Prevent raider shopkeepers from participating in nearby raids:
			if (entity instanceof Raider) {
				NMSManager.getProvider().setCanJoinRaid((Raider) entity, false);
			}

			// Apply sub type:
			this.onSpawn(entity);
		} else {
			// Failure:
			// Reset entity:
			E localEntity = this.entity;
			this.cleanUpEntity(localEntity);
			this.entity = null;

			// TODO Config option to delete the shopkeeper on failed spawn attempt? Check for this during shop creation?

			// Debug, if not already debugging and cooldown is over:
			boolean debug = (Settings.debug && !debuggingSpawn && (System.currentTimeMillis() - lastSpawnDebugging) > (5 * 60 * 1000)
					&& localEntity.isDead() && ChunkCoords.isChunkLoaded(localEntity.getLocation()));

			Log.warning("Failed to spawn shopkeeper entity: Entity dead: " + localEntity.isDead() + ", entity valid: " + localEntity.isValid()
					+ ", chunk loaded: " + ChunkCoords.isChunkLoaded(localEntity.getLocation()) + ", debug -> " + debug);

			// Debug entity spawning:
			if (debug) {
				// Print chunk's entity counts:
				EntityUtils.printEntityCounts(spawnLocation.getChunk());

				// Try again and log event activity:
				debuggingSpawn = true;
				lastSpawnDebugging = System.currentTimeMillis();
				Log.info("Trying again and logging event activity ..");

				// Log all events occurring during spawning, and their registered listeners:
				DebugListener debugListener = DebugListener.register(true, true);

				// Log creature spawn handling:
				EventDebugListener<CreatureSpawnEvent> spawnListener = new EventDebugListener<>(CreatureSpawnEvent.class, (priority, event) -> {
					LivingEntity spawnedEntity = event.getEntity();
					Log.info("  CreatureSpawnEvent (" + priority + "): " + "cancelled: " + event.isCancelled() + ", dead: " + spawnedEntity.isDead()
							+ ", valid: " + spawnedEntity.isValid() + ", chunk loaded: " + ChunkCoords.isChunkLoaded(spawnedEntity.getLocation()));
				});

				// Try to spawn the entity again:
				success = this.spawn();

				// Unregister listeners again:
				debugListener.unregister();
				spawnListener.unregister();
				debuggingSpawn = false;
				Log.info(".. Done. Successful: " + success);
			}
		}

		if (success) {
			// Reset respawn attempts counter and tick rate:
			respawnAttempts = 0;
			this.resetTickRate();
		}
		return success;
	}

	// Gets called after the entity was spawned. Can be used to apply any additionally configured mob-specific setup.
	protected void onSpawn(E entity) {
		// Nothing to do by default.
	}

	protected void overwriteAI() {
		// Setting the entity non-collidable:
		entity.setCollidable(false);
		// TODO Only required to handle the 'look-at-nearby-player' behavior. Maybe replace this with something own?
		NMSManager.getProvider().overwriteLivingEntityAI(entity);

		// Disable AI (also disables gravity) and replace it with our own handling:
		this.setNoAI(entity);

		if (Settings.silenceLivingShopEntities) {
			entity.setSilent(true);
		}
		if (Settings.disableGravity) {
			this.setNoGravity(entity);
			// When gravity is disabled, we may also able to disable collisions / the pushing of mobs via the noclip
			// flag. However, this might not properly work for Vex, since they disable their noclip again after their
			// movement.
			// TODO Still required? Bukkit's setCollidable API might actually work now.
			NMSManager.getProvider().setNoclip(entity);
		}
	}

	protected final void setNoAI(E entity) {
		entity.setAI(false);
		// Note on Bukkit's 'isAware' flag added in MC 1.15: Disables the AI logic similarly to NoAI, but the mob can
		// still move when being pushed around or due to gravity.
		// TODO The collidable API has been reworked to actually work now. Together with the isAware flag this could be
		// an alternative to using NoAI and then having to handle gravity on our own.
		// However, for now we prefer using NoAI. This might be safer in regards to potential future issues and also
		// automatically handles other cases, like players pushing entities around by hitting them.

		// Making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes that
		// it is currently falling:
		// TODO This can be removed once Spigot ignores NoAI entities.
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected final void setNoGravity(E entity) {
		entity.setGravity(false);

		// Making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes
		// that it is currently falling:
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected void cleanupAI() {
		// Disable AI:
		livingShops.getLivingEntityAI().removeEntity(entity);
	}

	@Override
	public void despawn() {
		if (entity == null) return;

		// Clean up entity:
		this.cleanUpEntity(entity);

		// Remove entity:
		entity.remove();
		entity = null;
		lastSpawnLocation = null;
	}

	@Override
	public Location getLocation() {
		if (entity == null) return null;
		return entity.getLocation();
	}

	// TICKING

	@Override
	public void tick() {
		super.tick();
		if (checkLimiter.request()) {
			this.check();

			// Indicate ticking activity for visualization:
			this.indicateTickActivity();
		}
	}

	private boolean isTickRateThrottled() {
		return (checkLimiter.getThreshold() == THROTTLED_CHECK_PERIOD_SECONDS);
	}

	private void throttleTickRate() {
		if (this.isTickRateThrottled()) return; // Already throttled
		Log.debug("Throttling tick rate");
		checkLimiter.setThreshold(THROTTLED_CHECK_PERIOD_SECONDS);
		checkLimiter.setRemainingThreshold(THROTTLED_CHECK_PERIOD_SECONDS + checkingOffset);
	}

	private void resetTickRate() {
		checkLimiter.setThreshold(CHECK_PERIOD_SECONDS);
		checkLimiter.setRemainingThreshold(checkingOffset);
	}

	private void check() {
		if (!this.isActive()) {
			this.respawnInactiveEntity();
		} else {
			this.teleportBackIfMoved();
			this.removePotionEffects();
		}
	}

	// True if the entity was respawned.
	private boolean respawnInactiveEntity() {
		assert !this.isActive();
		Log.debug(() -> "Shopkeeper (" + shopkeeper.getPositionString() + ") is missing, attemtping respawn");
		if (entity != null) {
			if (ChunkCoords.isSameChunk(shopkeeper.getLocation(), entity.getLocation())) {
				// The chunk was silently unloaded before:
				Log.debug(
						() -> "  Chunk got silently unloaded or mob got removed! (dead: " + entity.isDead() + ", valid: "
								+ entity.isValid() + ", chunk loaded: " + ChunkCoords.isChunkLoaded(entity.getLocation()) + ")"
				);
			}

			// Despawn (i.e. cleanup) the previously spawned but no longer active entity:
			this.despawn();
		}

		boolean spawned = this.spawn(); // This will load the chunk if necessary
		if (!spawned) {
			// TODO Maybe add a setting to remove shopkeeper if it can't be spawned a certain amount of times?
			Log.debug("  Respawn failed");
			respawnAttempts += 1;
			if (respawnAttempts >= MAX_RESPAWN_ATTEMPTS) {
				// Throttle the rate at which we attempt to respawn the entity:
				this.throttleTickRate();
			}
		} // Else: respawnAttempts and tick rate got reset.
		return spawned;
	}

	// This is not only relevant when gravity is enabled, but also to react to other plugins teleporting shopkeeper
	// entities around or enabling their AI again.
	private void teleportBackIfMoved() {
		assert this.isActive();
		assert entity != null && lastSpawnLocation != null;
		// Note: Comparing the entity's current location with the last spawn location (instead of freshly calculating
		// the 'intended' spawn location) not only provides a small performance benefit, but also ensures that
		// shopkeeper mobs don't start to fall if the block below them is broken and gravity is disabled (which is
		// confusing when gravity is supposed to be disabled).
		// However, to account for shopkeepers that have previously been placed above passable or non-full blocks, which
		// might also have been broken since then, we still place the shopkeeper mob up to one block below their
		// location when they are respawned (we just don't move them there dynamically during this check). If the mob is
		// supposed to dynamically move when the block below it is broken, gravity needs to be enabled.
		Location entityLoc = entity.getLocation();
		Location lastSpawnLocation = this.lastSpawnLocation;
		if (!entityLoc.getWorld().equals(lastSpawnLocation.getWorld()) || entityLoc.distanceSquared(lastSpawnLocation) > 0.2D) {
			// The squared distance 0.2 triggers for distances slightly below 0.5. Since we spawn the entity at the
			// center of the spawn block, this ensures that we teleport it back into place whenever it changes its
			// block.
			// Teleport back:
			Log.debug(DebugOptions.regularTickActivities, () -> "Shopkeeper (" + shopkeeper.getPositionString()
					+ ") out of place, teleporting back");
			lastSpawnLocation.setYaw(entityLoc.getYaw());
			lastSpawnLocation.setPitch(entityLoc.getPitch());
			entity.teleport(lastSpawnLocation);
			this.overwriteAI();
		}
	}

	private void removePotionEffects() {
		assert entity != null;
		for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
			entity.removePotionEffect(potionEffect.getType());
		}
	}

	public void teleportBack() {
		E entity = this.getEntity(); // Null if not spawned
		if (entity == null) return;
		assert lastSpawnLocation != null;

		Location lastSpawnLocation = this.lastSpawnLocation;
		Location entityLoc = entity.getLocation();
		lastSpawnLocation.setYaw(entityLoc.getYaw());
		lastSpawnLocation.setPitch(entityLoc.getPitch());
		entity.teleport(lastSpawnLocation);
	}

	// NAMING

	@Override
	public void setName(String name) {
		if (entity == null) return;
		this.applyName(entity, name);
	}

	protected void applyName(E entity, String name) {
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			name = Messages.nameplatePrefix + name;
			name = this.prepareName(name);
			// Set entity name plate:
			entity.setCustomName(name);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// Remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		if (entity == null) return null;
		return entity.getCustomName();
	}

	// EDITOR ACTIONS

	// No default actions.
}
