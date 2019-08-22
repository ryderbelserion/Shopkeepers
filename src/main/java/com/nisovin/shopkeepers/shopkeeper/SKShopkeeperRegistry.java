package com.nisovin.shopkeepers.shopkeeper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	private final SKShopkeepersPlugin plugin;

	// all shopkeepers:
	private final Map<UUID, AbstractShopkeeper> shopkeepersByUUID = new LinkedHashMap<>();
	private final Collection<AbstractShopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersByUUID.values());
	private final Map<Integer, AbstractShopkeeper> shopkeepersById = new LinkedHashMap<>();
	// TODO add index by world, to speed up functions that only affect the shopkeepers of a certain world (eg. world
	// save handling), maybe use a guava Table<String, ChunkCoords, List<AbstractShopkeeper>>
	// temporary shortcut to speed up metrics:
	private final Map<String, Integer> shopkeeperCountsByWorld = new HashMap<>();
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunk = new HashMap<>();
	// unmodifiable entries:
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeeperViewsByChunk = new HashMap<>();
	// unmodifiable map with unmodifiable entries:
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunkView = Collections.unmodifiableMap(shopkeeperViewsByChunk);
	private final Map<String, AbstractShopkeeper> activeShopkeepers = new HashMap<>(); // TODO remove this (?)
	private final Collection<AbstractShopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());
	private int playerShopCount = 0;

	// TODO: Since chunk loading is handled delayed, there might be multiple requests for loading the shopkeepers in a
	// chunk getting queued (if the chunk gets unloaded and reloaded frequently), and unloading may get invoked before
	// the shopkeepers got actually loaded.
	// This might not actually be an issue right now (shopkeepers only get loaded if the chunk is verified to currently
	// be loaded), but results in inconsistent (duplicate, wrong order) calls to Shopkeeper#onChunk(Un)Load
	// It would therefore be nicer to keep track for which chunks the shopkeepers got actually loaded and then ignore
	// duplicate load requests, and unload requests for not currently loaded chunks.

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// start teleporter task:
		this.startTeleporterTask();

		// start verifier task:
		if (Settings.enableSpawnVerifier) {
			this.startSpawnVerifierTask();
		}

		Bukkit.getPluginManager().registerEvents(new WorldListener(plugin, this), plugin);
	}

	public void onDisable() {
		// unload all shopkeepers:
		this.unloadAllShopkeepers();
		assert this.getAllShopkeepers().isEmpty();

		// reset, clearing (just in case):
		activeShopkeepers.clear();
		shopkeepersByChunk.clear();
		shopkeeperViewsByChunk.clear();
		shopkeepersByUUID.clear();
		shopkeepersById.clear();
		shopkeeperCountsByWorld.clear();
		playerShopCount = 0;
	}

	private void startTeleporterTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			List<AbstractShopkeeper> readd = new ArrayList<>();
			Iterator<Map.Entry<String, AbstractShopkeeper>> iter = activeShopkeepers.entrySet().iterator();
			while (iter.hasNext()) {
				AbstractShopkeeper shopkeeper = iter.next().getValue();
				boolean update = shopkeeper.check();
				if (update) {
					// if the shopkeeper had to be respawned its shop id changed:
					// this removes the entry which was stored with the old shop id and later adds back the
					// shopkeeper with its new id
					readd.add(shopkeeper);
					iter.remove();
				}
			}
			if (!readd.isEmpty()) {
				boolean dirty = false;
				for (AbstractShopkeeper shopkeeper : readd) {
					if (shopkeeper.isActive()) {
						this._activateShopkeeper(shopkeeper);
					}
					if (shopkeeper.isDirty()) dirty = true;
				}

				// shop objects might have been removed or respawned, request a save:
				if (dirty) {
					this.getShopkeeperStorage().save();
				}
			}
		}, 200, 200); // 10 seconds
	}

	private void startSpawnVerifierTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			int count = 0;
			boolean dirty = false;
			for (Entry<ChunkCoords, List<AbstractShopkeeper>> chunkEntry : this.getAllShopkeepersByChunks().entrySet()) {
				ChunkCoords chunk = chunkEntry.getKey();
				if (!chunk.isChunkLoaded()) continue;

				List<AbstractShopkeeper> shopkeepers = chunkEntry.getValue();
				for (AbstractShopkeeper shopkeeper : shopkeepers) {
					if (!shopkeeper.needsSpawning() || shopkeeper.isActive()) continue;

					// deactivate by old object id:
					this._deactivateShopkeeper(shopkeeper);
					// respawn:
					boolean spawned = shopkeeper.spawn();
					if (!spawned) {
						Log.debug("Spawn verifier: Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
						continue;
					}
					// activate with new object id:
					this._activateShopkeeper(shopkeeper);
					count++;
					if (shopkeeper.isDirty()) dirty = true;
				}
			}
			if (count > 0) {
				Log.debug("Spawn verifier: " + count + " shopkeepers respawned");
				if (dirty) {
					this.getShopkeeperStorage().save();
				}
			}
		}, 600, 1200); // 30,60 seconds
	}

	// SHOPKEEPER CREATION

	private SKShopkeeperStorage getShopkeeperStorage() {
		return plugin.getShopkeeperStorage();
	}

	@Override
	public AbstractShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		Validate.notNull(creationData, "CreationData is null!");
		AbstractShopType<?> abstractShopType = this.validateShopType(creationData.getShopType());

		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		int id = shopkeeperStorage.getNextShopkeeperId();
		AbstractShopkeeper shopkeeper = abstractShopType.createShopkeeper(id, creationData);
		if (shopkeeper == null) {
			// invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' returned null shopkeeper!");
		}
		// success:
		shopkeeperStorage.onShopkeeperIdUsed(id);
		if (shopkeeper.isDirty()) shopkeeperStorage.markDirty();
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.CREATED);
		return shopkeeper;
	}

	/**
	 * Recreates a shopkeeper by loading its previously saved data from the given config section.
	 * 
	 * @param shopType
	 *            the shop type
	 * @param id
	 *            the shopkeepers id
	 * @param configSection
	 *            the config section to load the shopkeeper data from
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	public AbstractShopkeeper loadShopkeeper(ShopType<?> shopType, int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		AbstractShopType<?> abstractShopType = this.validateShopType(shopType);
		Validate.notNull(configSection, "Missing config section!");
		Validate.isTrue(id >= 1, "Invalid id '" + id + "': Id has to be positive!");
		Validate.isTrue(this.getShopkeeperById(id) == null, "There is already a shopkeeper existing with this id: " + id);

		AbstractShopkeeper shopkeeper = abstractShopType.loadShopkeeper(id, configSection);
		if (shopkeeper == null) {
			// invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' returned null shopkeeper!");
		}
		// success:
		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		shopkeeperStorage.onShopkeeperIdUsed(id);
		if (shopkeeper.isDirty()) shopkeeperStorage.markDirty();
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.LOADED);
		return shopkeeper;
	}

	private AbstractShopType<?> validateShopType(ShopType<?> shopType) {
		Validate.notNull(shopType, "Missing shop type!");
		Validate.isTrue(shopType instanceof AbstractShopType,
				"Expecting an AbstractShopType, got " + shopType.getClass().getName());
		return (AbstractShopType<?>) shopType;
	}

	// LIFE CYCLE

	private void addShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperAddedEvent.Cause cause) {
		assert shopkeeper != null && !shopkeeper.isValid();

		// store by unique ids:
		shopkeepersByUUID.put(shopkeeper.getUniqueId(), shopkeeper);
		shopkeepersById.put(shopkeeper.getId(), shopkeeper);

		// add shopkeeper to chunk:
		ChunkCoords chunkCoords = shopkeeper.getChunkCoords();
		this.addShopkeeperToChunk(shopkeeper, chunkCoords);

		// update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount++;
		}

		// inform shopkeeper:
		shopkeeper.informAdded(cause);

		// call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperAddedEvent(shopkeeper, cause));

		// activate shopkeeper:
		if (!shopkeeper.needsSpawning()) {
			// activate shopkeeper once at registration:
			this._activateShopkeeper(shopkeeper);
		} else if (chunkCoords.isChunkLoaded()) {
			// activate shopkeeper due to loaded chunk:
			this.activateShopkeeper(shopkeeper);
		}
	}

	private void removeShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperRemoveEvent.Cause cause) {
		assert shopkeeper != null && shopkeeper.isValid() && cause != null;

		// deactivate shopkeeper:
		this.deactivateShopkeeper(shopkeeper, true);

		// call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperRemoveEvent(shopkeeper, cause));

		// inform shopkeeper:
		shopkeeper.informRemoval(cause);

		// remove shopkeeper by id and session id:
		shopkeepersByUUID.remove(shopkeeper.getUniqueId());
		shopkeepersById.remove(shopkeeper.getId());

		// remove shopkeeper from chunk:
		ChunkCoords chunkCoords = shopkeeper.getChunkCoords();
		this.removeShopkeeperFromChunk(shopkeeper, chunkCoords);

		// update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount--;
		}

		// remove shopkeeper from storage:
		this.getShopkeeperStorage().clearShopkeeperData(shopkeeper);
	}

	private void unloadShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null && shopkeeper.isValid();
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.UNLOAD);
	}

	public void unloadAllShopkeepers() {
		for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getAllShopkeepers())) {
			this.unloadShopkeeper(shopkeeper);
		}
	}

	public void deleteShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "Shopkeeper is null!");
		Validate.isTrue(shopkeeper.isValid(), "Shopkeeper is invalid!");
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.DELETE);
	}

	public void deleteAllShopkeepers() {
		for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getAllShopkeepers())) {
			this.deleteShopkeeper(shopkeeper);
		}
	}

	private void addShopkeeperToChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		List<AbstractShopkeeper> byChunk = shopkeepersByChunk.get(chunkCoords);
		if (byChunk == null) {
			byChunk = new ArrayList<>();
			shopkeepersByChunk.put(chunkCoords, byChunk);
			shopkeeperViewsByChunk.put(chunkCoords, Collections.unmodifiableList(byChunk));
		}
		byChunk.add(shopkeeper);

		// update shopkeeper count by world:
		String worldName = chunkCoords.getWorldName();
		Integer shopkeeperWorldCount = shopkeeperCountsByWorld.get(worldName);
		int newShopkeeperWorldCount = (shopkeeperWorldCount == null ? 0 : shopkeeperWorldCount.intValue()) + 1;
		if (newShopkeeperWorldCount > 0) {
			shopkeeperCountsByWorld.put(worldName, newShopkeeperWorldCount);
		} else {
			shopkeeperCountsByWorld.remove(worldName);
		}
	}

	private void removeShopkeeperFromChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		List<AbstractShopkeeper> byChunk = shopkeepersByChunk.get(chunkCoords);
		if (byChunk == null) return;
		if (byChunk.remove(shopkeeper)) {
			if (byChunk.isEmpty()) {
				shopkeepersByChunk.remove(chunkCoords);
				shopkeeperViewsByChunk.remove(chunkCoords);
			}

			// update shopkeeper count by world:
			String worldName = chunkCoords.getWorldName();
			Integer shopkeeperWorldCount = shopkeeperCountsByWorld.get(worldName);
			int newShopkeeperWorldCount = (shopkeeperWorldCount == null ? 0 : shopkeeperWorldCount.intValue()) - 1;
			if (newShopkeeperWorldCount > 0) {
				shopkeeperCountsByWorld.put(worldName, newShopkeeperWorldCount);
			} else {
				shopkeeperCountsByWorld.remove(worldName);
			}
		}
	}

	// ACTIVATION

	// performs some validation before actually activating a shopkeeper:
	// returns false if some validation failed
	private boolean _activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		if (objectId == null) {
			// currently only null is considered invalid,
			// prints 'null' to log then:
			Log.warning("Detected shopkeeper with invalid object id: " + objectId);
			return false;
		} else if (activeShopkeepers.containsKey(objectId)) {
			Log.warning("Detected shopkeepers with duplicate object id: " + objectId);
			return false;
		} else {
			// activate shopkeeper:
			activeShopkeepers.put(objectId, shopkeeper);
			return true;
		}
	}

	private boolean _deactivateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		return this._deactivateShopkeeper(shopkeeper, objectId);
	}

	private boolean _deactivateShopkeeper(AbstractShopkeeper shopkeeper, String objectId) {
		assert shopkeeper != null;
		if (objectId != null && activeShopkeepers.get(objectId) == shopkeeper) {
			activeShopkeepers.remove(objectId);
			return true;
		}
		return false;
	}

	private void activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (!shopkeeper.needsSpawning()) return;

		boolean activate = false;
		// spawn if not yet active:
		if (!shopkeeper.isActive()) {
			// deactivate shopkeeper by old shop object id, in case there is one:
			this._deactivateShopkeeper(shopkeeper);

			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				// activate with new object id:
				activate = true;
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		} else if (this.getActiveShopkeeper(shopkeeper.getObjectId()) == null) {
			// already active but missing activation, activate with current object id:
			activate = true;
		}
		if (activate) {
			// activate with current object id:
			this._activateShopkeeper(shopkeeper);
		}
	}

	private void deactivateShopkeeper(AbstractShopkeeper shopkeeper, boolean closeWindows) {
		assert shopkeeper != null;
		if (closeWindows) {
			// delayed closing of all open windows:
			shopkeeper.closeAllOpenWindows();
		}
		this._deactivateShopkeeper(shopkeeper);
		shopkeeper.despawn();
	}

	public void despawnAllShopkeepers() {
		// despawn all active shopkeepers:
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.despawn();
		}
	}

	// this can be used if the shopkeeper's object id has changed for some reason
	public void onShopkeeperObjectIdChanged(AbstractShopkeeper shopkeeper, String oldObjectId) {
		// deactivate by old object id:
		this._deactivateShopkeeper(shopkeeper, oldObjectId);
		// re-activate by new / current object id:
		this._activateShopkeeper(shopkeeper);
	}

	// SHOPKEEPERS BY CHUNK

	public void onShopkeeperMove(AbstractShopkeeper shopkeeper, ChunkCoords oldChunk) {
		assert oldChunk != null;
		ChunkCoords newChunk = shopkeeper.getChunkCoords();
		if (!oldChunk.equals(newChunk)) {
			// remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper, oldChunk);

			// add to new chunk:
			this.addShopkeeperToChunk(shopkeeper, newChunk);
		}
	}

	/**
	 * Loads (activates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return the number of shops in the affected chunk
	 */
	public int loadShopkeepersInChunk(Chunk chunk) {
		return this.loadShopkeepersInChunk(chunk, false);
	}

	/**
	 * Loads (activates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @param worldSaving
	 *            whether the shopkeepers get loaded due to the world saving finished
	 * @return the number of shops in the affected chunk
	 */
	public int loadShopkeepersInChunk(Chunk chunk, boolean worldSaving) {
		assert chunk != null;
		int affectedShops = 0;
		List<AbstractShopkeeper> shopkeepers = this.getShopkeepersInChunk(chunk);
		if (!shopkeepers.isEmpty()) {
			affectedShops = shopkeepers.size();
			Log.debug("Loading " + affectedShops + " shopkeepers in chunk "
					+ chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ()
					+ (worldSaving ? " (world saving finished)" : ""));
			boolean dirty = false;
			for (AbstractShopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk load:
				shopkeeper.onChunkLoad(worldSaving);

				// activate:
				this.activateShopkeeper(shopkeeper);
				if (shopkeeper.isDirty()) {
					dirty = true;
				}
			}

			if (dirty) {
				// save delayed:
				plugin.getShopkeeperStorage().saveDelayed();
			}
		}
		return affectedShops;
	}

	/**
	 * Unloads (deactivates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return the number of shops in the affected chunk
	 */
	public int unloadShopkeepersInChunk(Chunk chunk) {
		return unloadShopkeepersInChunk(chunk, false);
	}

	/**
	 * Unloads (deactivates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @param worldSaving
	 *            whether the shopkeepers get unloaded due to the world saving
	 * @return the number of shops in the affected chunk
	 */
	public int unloadShopkeepersInChunk(Chunk chunk, boolean worldSaving) {
		assert chunk != null;
		int affectedShops = 0;
		List<AbstractShopkeeper> shopkeepers = this.getShopkeepersInChunk(chunk);
		if (!shopkeepers.isEmpty()) {
			affectedShops = shopkeepers.size();
			Log.debug("Unloading " + affectedShops + " shopkeepers in chunk "
					+ chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ()
					+ (worldSaving ? " (world saving)" : ""));
			for (AbstractShopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk unload:
				shopkeeper.onChunkUnload(worldSaving);

				// skip shopkeepers which are kept active all the time (ex. citizens shops):
				if (!shopkeeper.needsSpawning()) continue;
				// if world save: skip shopkeepers that do not need to be despawned:
				if (worldSaving && !shopkeeper.getShopObject().getType().despawnDuringWorldSaves()) {
					continue;
				}

				// deactivate:
				this.deactivateShopkeeper(shopkeeper, false);
			}
		}
		return affectedShops;
	}

	/**
	 * Loads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 * @return the number of loaded shopkeepers
	 */
	public int loadShopkeepersInWorld(World world) {
		return this.loadShopkeepersInWorld(world, false);
	}

	/**
	 * Loads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 * @param worldSaving
	 *            whether the shopkeepers get loaded due to the world saving finished
	 * @return the number of loaded shopkeepers
	 */
	public int loadShopkeepersInWorld(World world, boolean worldSaving) {
		assert world != null;
		int affectedShops = 0;
		String worldName = world.getName();
		for (ChunkCoords chunkCoords : this.getAllShopkeepersByChunks().keySet()) {
			if (!chunkCoords.getWorldName().equals(worldName)) continue; // different world
			Chunk chunk = chunkCoords.getChunk();
			if (chunk == null) continue; // not loaded
			affectedShops += this.loadShopkeepersInChunk(chunk, worldSaving);
		}
		Log.debug("Loaded " + affectedShops + " shopkeepers in world " + world.getName()
				+ (worldSaving ? " (world saving finished)" : ""));
		return affectedShops;
	}

	public void loadShopkeepersInAllWorlds() {
		// activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		for (World world : Bukkit.getWorlds()) {
			this.loadShopkeepersInWorld(world);
		}
	}

	/**
	 * Unloads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 * @return the number of unloaded shopkeepers
	 */
	public int unloadShopkeepersInWorld(World world) {
		return this.unloadShopkeepersInWorld(world, false);
	}

	/**
	 * Unloads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 * @param worldSaving
	 *            whether the shopkeepers get loaded due to the world saving
	 * @return the number of unloaded shopkeepers
	 */
	public int unloadShopkeepersInWorld(World world, boolean worldSaving) {
		assert world != null;
		int affectedShops = 0;
		String worldName = world.getName();
		for (ChunkCoords chunkCoords : this.getAllShopkeepersByChunks().keySet()) {
			if (!chunkCoords.getWorldName().equals(worldName)) continue; // different world
			Chunk chunk = chunkCoords.getChunk();
			if (chunk == null) continue; // not loaded
			affectedShops += this.unloadShopkeepersInChunk(chunk, worldSaving);
		}
		Log.debug("Unloaded " + affectedShops + " shopkeepers in world " + world.getName()
				+ (worldSaving ? " (world saving)" : ""));
		return affectedShops;
	}

	// QUERYING

	public int getPlayerShopCount() {
		return playerShopCount;
	}

	public Map<String, Integer> getShopkeeperCountsByWorld() {
		return Collections.unmodifiableMap(shopkeeperCountsByWorld);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByUniqueId(UUID shopkeeperUUID) {
		return shopkeepersByUUID.get(shopkeeperUUID);
	}

	@Override
	public AbstractShopkeeper getShopkeeperById(int shopkeeperId) {
		return shopkeepersById.get(shopkeeperId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByName(String shopName) {
		if (shopName == null) return null;
		shopName = TextUtils.stripColor(shopName);
		shopName = StringUtils.normalize(shopName);
		for (AbstractShopkeeper shopkeeper : this.getAllShopkeepers()) {
			String shopkeeperName = shopkeeper.getName();
			if (shopkeeperName == null) continue;
			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			if (shopkeeperName.equals(shopName)) {
				return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public AbstractShopkeeper getActiveShopkeeper(String objectId) {
		return activeShopkeepers.get(objectId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractEntityShopObjectType) {
				String objectId = ((AbstractEntityShopObjectType<?>) shopObjectType).createObjectId(entity);
				AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
				if (shopkeeper != null) return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return (this.getShopkeeperByEntity(entity) != null);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractBlockShopObjectType) {
				String objectId = ((AbstractBlockShopObjectType<?>) shopObjectType).createObjectId(block);
				AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
				if (shopkeeper != null) return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeperByBlock(block) != null);
	}

	@Override
	public Collection<AbstractShopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public Map<ChunkCoords, List<AbstractShopkeeper>> getAllShopkeepersByChunks() {
		return shopkeepersByChunkView;
	}

	@Override
	public Collection<AbstractShopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public List<AbstractShopkeeper> getShopkeepersAtLocation(Location location) {
		Validate.notNull(location, "Location is null!");
		Validate.notNull(location.getWorld(), "Location's world is null!");
		String worldName = location.getWorld().getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		for (AbstractShopkeeper shopkeeper : this.getAllShopkeepers()) {
			if (shopkeeper.getWorldName().equals(worldName) && shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				shopkeepers.add(shopkeeper);
			}
		}
		return shopkeepers;
	}

	@Override
	public List<AbstractShopkeeper> getShopkeepersInChunk(Chunk chunk) {
		return this.getShopkeepersInChunk(new ChunkCoords(chunk));
	}

	@Override
	public List<AbstractShopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords) {
		List<AbstractShopkeeper> byChunk = shopkeepersByChunkView.get(chunkCoords);
		if (byChunk == null) return Collections.emptyList();
		return byChunk; // unmodifiable already
	}

	@Override
	public List<AbstractShopkeeper> getShopkeepersInWorld(World world, boolean onlyLoadedChunks) {
		Validate.notNull(world, "World is null!");
		List<AbstractShopkeeper> shopkeepersInWorld = new ArrayList<>();
		String worldName = world.getName();
		for (Entry<ChunkCoords, List<AbstractShopkeeper>> byChunkEntry : this.getAllShopkeepersByChunks().entrySet()) {
			ChunkCoords chunkCoords = byChunkEntry.getKey();
			if (!chunkCoords.getWorldName().equals(worldName)) continue; // different world
			if (onlyLoadedChunks && !chunkCoords.isChunkLoaded()) continue; // not loaded
			shopkeepersInWorld.addAll(byChunkEntry.getValue());
		}
		return Collections.unmodifiableList(shopkeepersInWorld);
	}

	public int countShopsOfPlayer(Player player) {
		int count = 0;
		for (Shopkeeper shopkeeper : this.getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper) shopkeeper).isOwner(player)) {
				count++;
			}
		}
		return count;
	}
}
