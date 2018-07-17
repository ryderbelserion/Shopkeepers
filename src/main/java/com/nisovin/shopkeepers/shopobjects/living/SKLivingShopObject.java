package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class SKLivingShopObject extends AbstractEntityShopObject implements LivingShopObject {

	protected final LivingShops livingShops;
	private final SKLivingShopObjectType<?> livingObjectType;
	private LivingEntity entity;
	private String uuid;
	private int respawnAttempts = 0;

	protected SKLivingShopObject(LivingShops livingShops, SKLivingShopObjectType<?> livingObjectType, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
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
		if (configSection.contains("uuid")) {
			this.uuid = configSection.getString("uuid");
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// let's save last known uuid nevertheless, for the case that the entity somehow wasn't properly removed before
		// (which seems to still happen sometimes during server shutdowns)
		if (uuid != null && !uuid.isEmpty()) {
			configSection.set("uuid", uuid);
		}
	}

	// ACTIVATION

	@Override
	public LivingEntity getEntity() {
		// is-active check:
		// note: some spigot versions didn't check the isDead flag inside isValid:
		// note: isValid-flag gets set at the tick after handling all queued chunk unloads, so isChunkLoaded check is
		// needed if we check during chunk unloads and the entity in question might be in another chunk than the
		// currently unloaded one
		if (entity != null && !entity.isDead() && entity.isValid() && ChunkCoords.isChunkLoaded(entity.getLocation())) {
			return entity;
		}
		return null;
	}

	@Override
	public boolean isActive() {
		return (this.getEntity() != null);
	}

	@Override
	public String getId() {
		return this.getType().createObjectId(this.getEntity());
	}

	protected void assignShopkeeperMetadata(LivingEntity entity) {
		entity.setMetadata("shopkeeper", new FixedMetadataValue(ShopkeepersPlugin.getInstance(), true));
	}

	protected void removeShopkeeperMetadata(LivingEntity entity) {
		entity.removeMetadata("shopkeeper", ShopkeepersPlugin.getInstance());
	}

	// returns true if we find a valid entity:
	protected boolean searchOldEntity() {
		assert !this.isActive();
		if (uuid != null && !uuid.isEmpty()) {
			Location spawnLocation = this.getSpawnLocation();
			// this only checks loaded neighbor chunks
			// remaining issues after server crashes or with missing chunk unload events:
			// * depending on chunk loading order, old entities in neighboring chunks might not get found
			// * if the entity is further away than 1 chunk we we won't find it again, and there is no real solution to
			// this, because the entity could be anywhere now (even if we have a reference to the old entity instance
			// after an unnoticed chunk unload, because after a subsequent chunk load the actual entity can wander away
			// into a completely different chunk)
			for (Entity e : Utils.getNearbyChunkEntities(spawnLocation.getChunk(), 1, false, this.getEntityType())) {
				if (e.isValid() && !e.isDead() && e.getUniqueId().toString().equalsIgnoreCase(uuid)) {
					Log.debug("  Found old shopkeeper entity, using it now");
					entity = (LivingEntity) e;
					// entity.setHealth(entity.getMaxHealth());
					entity.teleport(spawnLocation);
					assert this.isActive(); // let's assume that the found entity is still valid since we found it
					return true;
				}
			}
		}
		return false;
	}

	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		double offset = 0.0D;
		// is gravity active? -> spawn slightly above the ground:
		if (!Settings.disableGravity && !(Settings.useLegacyMobBehavior && this.isNoAIMobType() && NMSManager.getProvider().isNoAIDisablingGravity())) {
			offset = 0.5D;
		}
		return new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + offset, shopkeeper.getZ() + 0.5D);
	}

	@Override
	public boolean spawn() {
		// check if our current old entity is still valid:
		if (this.isActive()) return true;
		if (entity != null) {
			// clean up metadata before replacing the currently stored entity with a new one:
			this.removeShopkeeperMetadata(entity);
		}

		// prepare location:
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		Location spawnLocation = this.getSpawnLocation();
		// find old shopkeeper entity, else spawn a new one:
		if (!this.searchOldEntity()) {
			// TODO check if the block is passable before spawning there?
			// try to bypass entity-spawn blocking plugins:
			EntityType entityType = this.getEntityType();
			livingShops.forceCreatureSpawn(spawnLocation, entityType);
			entity = (LivingEntity) world.spawnEntity(spawnLocation, entityType);
			uuid = entity.getUniqueId().toString();
			shopkeeper.markDirty();
		}

		if (this.isActive()) {
			// assign metadata for easy identification by other plugins:
			this.assignShopkeeperMetadata(entity);
			this.setName(shopkeeper.getName());

			// configure some entity attributes:
			entity.eject(); // some entities might automatically mount on nearby entities (like baby zombies on chicken)
			entity.setRemoveWhenFarAway(false);
			entity.setCanPickupItems(false);

			// disable breeding:
			if (entity instanceof Ageable) {
				Ageable ageable = ((Ageable) entity);
				ageable.setBreed(false);
				ageable.setAgeLock(true);
			}

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}

			// apply sub type:
			this.applySubType();

			// overwrite AI:
			this.overwriteAI();

			// success:
			return true;
		} else {
			// failure:
			entity = null;
			return false;
		}
	}

	// some mobs will always get their AI disabled in order to properly work:
	protected boolean isNoAIMobType() {
		switch (livingObjectType.getEntityType()) {
		case BAT:
		case ENDER_DRAGON:
		case ENDERMAN:
		case WITHER:
		case SILVERFISH:
		case BLAZE:
			return true;
		default:
			return false;
		}
	}

	protected void overwriteAI() {
		// setting the entity non-collidable:
		entity.setCollidable(false);
		NMSManager.getProvider().overwriteLivingEntityAI(entity);

		if (!Settings.useLegacyMobBehavior) {
			// disable AI (also disables gravity) and replace it with our own handling:
			this.setNoAI(entity);

			if (NMSManager.getProvider().supportsCustomMobAI()) {
				livingShops.getLivingEntityAI().addEntity(entity);
			}
		}

		if (Settings.silenceLivingShopEntities) {
			entity.setSilent(true);
		}
		if (Settings.disableGravity) {
			this.setNoGravity(entity);
			// when gravity gets disabled, we might be able to also disable collisions/pushing of mobs via noclip:
			NMSManager.getProvider().setNoclip(entity);
		}

		// set the NoAI tag always for certain entity types:
		if (this.isNoAIMobType()) {
			this.setNoAI(entity);
		}
	}

	protected final void setNoAI(LivingEntity entity) {
		entity.setAI(false);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes that
		// it is currently falling:
		// TODO this can be removed once spigot ignores NoAI entities
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected final void setNoGravity(org.bukkit.entity.Entity entity) {
		entity.setGravity(false);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes
		// that it is currently falling:
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected void cleanupAI() {
		// disable AI:
		livingShops.getLivingEntityAI().removeEntity(entity);
	}

	@Override
	public void despawn() {
		if (entity != null) {
			// note: in case the entity is not active but not in the same chunk, it was probably pushed into a different
			// and now already unloaded chunk, in which case it got already removed inside the chunk-unload listener
			if (!this.isActive() && ChunkCoords.isSameChunk(shopkeeper.getLocation(), entity.getLocation())) {
				Log.debug("Chunk was silently unloaded at (" + shopkeeper.getPositionString() + "): Loading it now to remove old entity");
				World world = Bukkit.getWorld(shopkeeper.getWorldName());
				if (world != null) {
					this.searchOldEntity(); // this will load the chunk
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (for now let's assume
					// that the server can handle this automatically)
					// Chunk chunk = location.getChunk();
					// world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true);
				}
			}

			// disable AI:
			this.cleanupAI();

			// cleanup metadata:
			this.removeShopkeeperMetadata(entity);

			// remove entity:
			entity.remove();
			entity = null;
			// TODO chunk loading and removal might not work during server shutdown.. :(
			// so we are now storing the last known entity uuid
		}
	}

	@Override
	public Location getLocation() {
		if (this.isActive()) {
			return entity.getLocation();
		} else {
			return null;
		}
	}

	@Override
	public boolean check() {
		if (!this.isActive()) {
			Log.debug("Shopkeeper (" + shopkeeper.getPositionString() + ") missing, triggering respawn now");
			boolean silentlyUnloaded = (entity != null && ChunkCoords.isSameChunk(shopkeeper.getLocation(), entity.getLocation()));
			if (silentlyUnloaded) {
				// the chunk was silently unloaded before:
				Log.debug("  Chunk was silently unloaded before: Loading it now and requesting controlled unload");
			}
			boolean spawned = this.spawn(); // this will load the chunk if necessary
			if (spawned) {
				respawnAttempts = 0;
				if (silentlyUnloaded) {
					String worldName = shopkeeper.getWorldName();
					World world = Bukkit.getWorld(worldName);
					Location location = this.getSpawnLocation();
					Chunk chunk = location.getChunk();
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (in order to not keep the
					// chunks loaded by constantly calling of this method)
					world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true);
				}
				return true;
			} else {
				// TODO maybe add a setting to remove shopkeeper if it can't be spawned a certain amount of times?
				Log.debug("  Respawn failed");
				return (++respawnAttempts > 5);
			}
		} else {
			Location entityLoc = entity.getLocation();
			Location spawnLocation = this.getSpawnLocation();
			spawnLocation.setYaw(entityLoc.getYaw());
			spawnLocation.setPitch(entityLoc.getPitch());
			if (!entityLoc.getWorld().equals(spawnLocation.getWorld()) || entityLoc.distanceSquared(spawnLocation) > 0.4D) {
				// teleport back:
				entity.teleport(spawnLocation);
				this.overwriteAI();
				Log.debug("Shopkeeper (" + shopkeeper.getPositionString() + ") out of place, teleported back");
			}

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}
			return false;
		}
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return 32;
	}

	@Override
	public void setName(String name) {
		if (!this.isActive()) return;
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.prepareName(name);
			// set entity name plate:
			entity.setCustomName(name);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		if (!this.isActive()) return null;
		return entity.getCustomName();
	}

	// SUB TYPES
	// not supported by default

	protected void applySubType() {
		// nothing to do by default
	}

	// OTHER PROPERTIES

	@Override
	public void equipItem(ItemStack item) {
		if (this.isActive()) {
			entity.getEquipment().setItemInMainHand(item);
			entity.getEquipment().setItemInMainHandDropChance(0);
		}
	}
}
