package com.nisovin.shopkeepers.registry;

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
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.registry.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.registry.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shoptypes.ShopType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.SignShop;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shoptypes.AbstractShopType;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.Log;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	private final SKShopkeepersPlugin plugin;

	// all shopkeepers:
	private final Map<UUID, AbstractShopkeeper> shopkeepersByUUID = new LinkedHashMap<>();
	private final Collection<AbstractShopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersByUUID.values());
	private final Map<Integer, AbstractShopkeeper> shopkeepersById = new LinkedHashMap<>();
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunk = new HashMap<>();
	// unmodifiable entries:
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeeperViewsByChunk = new HashMap<>();
	// unmodifiable map with unmodifiable entries:
	private final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunkView = Collections.unmodifiableMap(shopkeeperViewsByChunk);
	private final Map<String, AbstractShopkeeper> activeShopkeepers = new HashMap<>(); // TODO remove this (?)
	private final Collection<AbstractShopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// start teleporter task:
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			List<AbstractShopkeeper> readd = new ArrayList<>();
			Iterator<Map.Entry<String, AbstractShopkeeper>> iter = activeShopkeepers.entrySet().iterator();
			while (iter.hasNext()) {
				AbstractShopkeeper shopkeeper = iter.next().getValue();
				boolean update = shopkeeper.check();
				if (update) {
					// if the shopkeeper had to be respawned its shop id changed:
					// this removes the entry which was stored with the old shop id and later adds back the
					// shopkeeper with it's new id
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
					plugin.getShopkeeperStorage().save();
				}
			}
		}, 200, 200); // 10 seconds

		// start verifier task:
		if (Settings.enableSpawnVerifier) {
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
							Log.debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
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
						plugin.getShopkeeperStorage().save();
					}
				}
			}, 600, 1200); // 30,60 seconds
		}
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
	}

	private void removeShopkeeperFromChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		List<AbstractShopkeeper> byChunk = shopkeepersByChunk.get(chunkCoords);
		if (byChunk == null) return;
		if (byChunk.remove(shopkeeper) && byChunk.isEmpty()) {
			shopkeepersByChunk.remove(chunkCoords);
			shopkeeperViewsByChunk.remove(chunkCoords);
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
		if (activeShopkeepers.get(objectId) == shopkeeper) {
			activeShopkeepers.remove(objectId);
			return true;
		}
		return false;
	}

	private void activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (shopkeeper.needsSpawning() && !shopkeeper.isActive()) {
			// deactivate shopkeeper by old shop object id, in case there is one:
			if (this._deactivateShopkeeper(shopkeeper)) {
				if (Settings.debug && shopkeeper.getShopObject() instanceof LivingEntityShop) {
					LivingEntityShop livingShop = (LivingEntityShop) shopkeeper.getShopObject();
					LivingEntity oldEntity = livingShop.getEntity();
					Log.debug("Old, active shopkeeper was found (unloading probably has been skipped earlier): "
							+ (oldEntity == null ? "null" : (oldEntity.getUniqueId() + " | " + (oldEntity.isDead() ? "dead | " : "alive | ")
									+ (oldEntity.isValid() ? "valid" : "invalid"))));
				}
			}

			// spawn and activate:
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				// activate with new object id:
				this._activateShopkeeper(shopkeeper);
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
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
		assert chunk != null;
		int affectedShops = 0;
		List<AbstractShopkeeper> shopkeepers = this.getShopkeepersInChunk(chunk);
		if (!shopkeepers.isEmpty()) {
			affectedShops = shopkeepers.size();
			Log.debug("Loading " + affectedShops + " shopkeepers in chunk "
					+ chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
			boolean dirty = false;
			for (AbstractShopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk load:
				shopkeeper.onChunkLoad();

				// activate:
				this.activateShopkeeper(shopkeeper);
				if (shopkeeper.isDirty()) dirty = true;
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
		assert chunk != null;
		int affectedShops = 0;
		List<AbstractShopkeeper> shopkeepers = this.getShopkeepersInChunk(chunk);
		if (!shopkeepers.isEmpty()) {
			affectedShops = shopkeepers.size();
			Log.debug("Unloading " + affectedShops + " shopkeepers in chunk "
					+ chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
			for (AbstractShopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk unload:
				shopkeeper.onChunkUnload();

				// skip shopkeepers which are kept active all the time (ex. sign, citizens shops):
				if (!shopkeeper.needsSpawning()) continue;

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
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.loadShopkeepersInChunk(chunk);
		}
		Log.debug("Loaded " + affectedShops + " shopkeepers in world " + world.getName());
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
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.unloadShopkeepersInChunk(chunk);
		}
		Log.debug("Unloaded " + affectedShops + " shopkeepers in world " + world.getName());
		return affectedShops;
	}

	// QUERYING

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
		shopName = ChatColor.stripColor(shopName);
		for (AbstractShopkeeper shopkeeper : this.getAllShopkeepers()) {
			String shopkeeperName = shopkeeper.getName();
			if (shopkeeperName != null && ChatColor.stripColor(shopkeeperName).equalsIgnoreCase(shopName)) {
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
		// check if the entity is a living entity shopkeeper:
		AbstractShopkeeper shopkeeper = this.getLivingEntityShopkeeper(entity);
		if (shopkeeper != null) return shopkeeper;
		// check if the entity is a citizens npc shopkeeper:
		return this.getCitizensShopkeeper(entity);
	}

	public AbstractShopkeeper getLivingEntityShopkeeper(Entity entity) {
		if (entity == null) return null;
		return this.getActiveShopkeeper(LivingEntityShop.getId(entity));
	}

	public AbstractShopkeeper getCitizensShopkeeper(Entity entity) {
		if (entity == null) return null;
		Integer npcId = CitizensHandler.getNPCId(entity);
		if (npcId == null) return null;
		return this.getActiveShopkeeper(CitizensShop.getId(npcId));
	}

	@Override
	public AbstractShopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		return this.getActiveShopkeeper(SignShop.getId(block));
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return this.getShopkeeperByEntity(entity) != null;
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
		if (onlyLoadedChunks) {
			for (Chunk chunk : world.getLoadedChunks()) {
				shopkeepersInWorld.addAll(this.getShopkeepersInChunk(chunk));
			}
		} else {
			String worldName = world.getName();
			for (Entry<ChunkCoords, List<AbstractShopkeeper>> byChunkEntry : this.getAllShopkeepersByChunks().entrySet()) {
				if (byChunkEntry.getKey().getWorldName().equals(worldName)) {
					shopkeepersInWorld.addAll(byChunkEntry.getValue());
				}
			}
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
