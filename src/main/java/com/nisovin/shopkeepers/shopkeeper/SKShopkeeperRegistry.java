package com.nisovin.shopkeepers.shopkeeper;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.block.DefaultBlockShopObjectIds;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.entity.DefaultEntityShopObjectIds;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	private static final class ChunkShopkeepers {

		final WorldShopkeepers worldEntry;
		final ChunkCoords chunkCoords;
		// List instead of set or map: We don't expect there to be excessive amounts of shopkeepers inside a single
		// chunk, so removal from the list should be sufficiently fast.
		final List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		// Note: The chunk stays marked as active during the temporary despawning of shopkeepers during world saves.
		boolean active;
		BukkitTask activationTask = null;

		ChunkShopkeepers(WorldShopkeepers worldEntry, ChunkCoords chunkCoords, boolean active) {
			assert worldEntry != null && chunkCoords != null;
			this.worldEntry = worldEntry;
			this.chunkCoords = chunkCoords;
			this.active = active;
		}

		boolean isActivationPending() {
			return (activationTask != null);
		}

		void cancelActivationTask() {
			if (activationTask == null) return; // No activation pending
			activationTask.cancel();
			activationTask = null;
		}

		void cleanUp() {
			this.cancelActivationTask();
		}
	}

	private static final class WorldShopkeepers {

		final String worldName;
		final Map<ChunkCoords, ChunkShopkeepers> shopkeepersByChunk = new HashMap<>();
		// Unmodifiable entries:
		final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeeperViewsByChunk = new HashMap<>();
		// Unmodifiable map with unmodifiable entries:
		final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunkView = Collections.unmodifiableMap(shopkeeperViewsByChunk);
		int shopkeeperCount = 0;
		BukkitTask worldSaveRespawnTask = null;

		// Note: Already unmodifiable.
		final Set<AbstractShopkeeper> shopkeepersView = new AbstractSet<AbstractShopkeeper>() {
			@Override
			public Iterator<AbstractShopkeeper> iterator() {
				return shopkeepersByChunk.values().stream()
						.flatMap(chunkEntry -> chunkEntry.shopkeepers.stream())
						.iterator();
			}

			@Override
			public int size() {
				return shopkeeperCount;
			}
		};
		// Note: Already unmodifiable.
		final Set<ChunkCoords> activeChunksView = new AbstractSet<ChunkCoords>() {
			@Override
			public Iterator<ChunkCoords> iterator() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.map(chunkEntry -> chunkEntry.chunkCoords)
						.iterator();
			}

			@Override
			public int size() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.mapToInt(chunkEntry -> 1)
						.sum();
			}
		};
		// Note: Already unmodifiable.
		final Set<AbstractShopkeeper> shopkeepersInActiveChunksView = new AbstractSet<AbstractShopkeeper>() {
			@Override
			public Iterator<AbstractShopkeeper> iterator() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.flatMap(chunkEntry -> chunkEntry.shopkeepers.stream())
						.iterator();
			}

			@Override
			public int size() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.mapToInt(chunkEntry -> chunkEntry.shopkeepers.size())
						.sum();
			}
		};

		WorldShopkeepers(String worldName) {
			assert worldName != null;
			this.worldName = worldName;
		}

		ChunkShopkeepers addShopkeeper(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
			assert shopkeeper != null && chunkCoords != null;
			// Assert: World name matches this world entry.
			// Assert: Shopkeeper is not yet contained.
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			if (chunkEntry == null) {
				// If the chunk is currently loaded, the chunk entry gets initialized as active:
				boolean chunkLoaded = chunkCoords.isChunkLoaded();
				chunkEntry = new ChunkShopkeepers(this, chunkCoords, chunkLoaded);
				shopkeepersByChunk.put(chunkCoords, chunkEntry);
				shopkeeperViewsByChunk.put(chunkCoords, Collections.unmodifiableList(chunkEntry.shopkeepers));
			}
			chunkEntry.shopkeepers.add(shopkeeper);
			shopkeeperCount += 1;
			return chunkEntry;
		}

		ChunkShopkeepers removeShopkeeper(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
			assert shopkeeper != null && chunkCoords != null;
			// Assert: World name matches this world entry.
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			if (chunkEntry == null) return null; // Could not find shopkeeper
			if (chunkEntry.shopkeepers.remove(shopkeeper)) {
				shopkeeperCount -= 1;
				if (chunkEntry.shopkeepers.isEmpty()) {
					chunkEntry.cleanUp();
					shopkeepersByChunk.remove(chunkCoords);
					shopkeeperViewsByChunk.remove(chunkCoords);
				}
			}
			return chunkEntry;
		}

		boolean isWorldSaveRespawnPending() {
			return (worldSaveRespawnTask != null);
		}

		void cancelWorldSaveRespawnTask() {
			if (worldSaveRespawnTask == null) return;
			worldSaveRespawnTask.cancel();
			worldSaveRespawnTask = null;
		}

		void cleanUp() {
			this.cancelWorldSaveRespawnTask();
		}
	}

	private static final long CHUNK_ACTIVATION_DELAY_TICKS = 2;

	private final SKShopkeepersPlugin plugin;

	// All shopkeepers:
	private final Map<UUID, AbstractShopkeeper> shopkeepersByUUID = new LinkedHashMap<>();
	private final Collection<AbstractShopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersByUUID.values());
	private final Map<Integer, AbstractShopkeeper> shopkeepersById = new HashMap<>();

	// TODO Shopkeepers by name TreeMap to speedup name lookups and prefix matching?
	// TODO TreeMaps for shopkeeper owners by name and uuid to speedup prefix matching?

	// Virtual shopkeepers:
	// Set: Allows for fast removal.
	private final Set<AbstractShopkeeper> virtualShopkeepers = new LinkedHashSet<>();
	private final Collection<AbstractShopkeeper> virtualShopkeepersView = Collections.unmodifiableCollection(virtualShopkeepers);

	// By world name:
	private final Map<String, WorldShopkeepers> shopkeepersByWorld = new LinkedHashMap<>();
	private final Set<String> shopkeeperWorldsView = Collections.unmodifiableSet(shopkeepersByWorld.keySet());

	// Player shopkeepers:
	private int playerShopCount = 0;
	// Note: Already unmodifiable.
	private final Set<AbstractPlayerShopkeeper> allPlayerShopkeepersView = new AbstractSet<AbstractPlayerShopkeeper>() {
		@Override
		public Iterator<AbstractPlayerShopkeeper> iterator() {
			return getAllShopkeepers().stream()
					.filter(shopkeeper -> shopkeeper instanceof PlayerShopkeeper)
					.map(shopkeeper -> (AbstractPlayerShopkeeper) shopkeeper)
					.iterator();
		}

		@Override
		public int size() {
			return playerShopCount;
		}
	};

	// TODO This may become out-of-sync if shop objects get despawned or removed independently, problem? potential
	// memory leak?
	// -> Gets cleaned up by 'teleporter' task currently which periodically checks all activeShopkeepers entries.
	// 'active': With active shop object (i.e. after successful spawning).
	private final Map<String, AbstractShopkeeper> activeShopkeepers = new HashMap<>();
	private final Collection<AbstractShopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// Start shopkeeper ticking task:
		this.startShopkeeperTickTask();

		// Start teleporter task:
		this.startTeleporterTask();

		Bukkit.getPluginManager().registerEvents(new WorldListener(this), plugin);
	}

	public void onDisable() {
		// Unload all shopkeepers:
		this.unloadAllShopkeepers();
		assert this.getAllShopkeepers().isEmpty();

		// Reset, clearing (just in case):
		shopkeepersByUUID.clear();
		shopkeepersById.clear();
		shopkeepersByWorld.clear();
		virtualShopkeepers.clear();
		activeShopkeepers.clear();
		playerShopCount = 0;
	}

	// PERIODIC TASKS

	private void startTeleporterTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			List<AbstractShopkeeper> readd = new ArrayList<>();
			Iterator<Map.Entry<String, AbstractShopkeeper>> iter = activeShopkeepers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, AbstractShopkeeper> entry = iter.next();
				AbstractShopkeeper shopkeeper = entry.getValue();
				boolean update = shopkeeper.getShopObject().check();
				if (update) { // TODO Remove return boolean and instead compare old with current object id?
					// If the shopkeeper had to be respawned its shop id changed.
					// This removes the entry which was stored with the old shop id and later adds back the
					// shopkeeper with its new id.
					readd.add(shopkeeper);
					iter.remove();
				}
			}
			if (!readd.isEmpty()) {
				boolean dirty = false;
				for (AbstractShopkeeper shopkeeper : readd) {
					if (shopkeeper.getShopObject().isActive()) {
						this._activateShopkeeper(shopkeeper);
					}
					if (shopkeeper.isDirty()) dirty = true;
				}

				// Shop objects might have been removed or respawned, request a save:
				if (dirty) {
					this.getShopkeeperStorage().save();
				}
			}
		}, 200, 200); // 10 seconds
	}


	// TICKING

	private void startShopkeeperTickTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			// Prevents concurrent modification errors by making copies of the iterated data:
			// The copies are required because the worlds, active chunks and chunk shopkeepers may change if shopkeepers
			// get removed or chunks get loaded (which cannot be safely avoided).
			boolean dirty = false;
			for (String worldName : new ArrayList<>(this.getWorldsWithShopkeepers())) {
				for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getShopkeepersInActiveChunks(worldName))) {
					if (!shopkeeper.isValid()) continue; // Skip if no longer valid
					shopkeeper.tick();
					if (shopkeeper.isDirty()) {
						dirty = true;
					}
				}
			}
			if (dirty) {
				this.getShopkeeperStorage().save();
			}
		}, 20L, 20L); // 1 second
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
			// Invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' created null shopkeeper!");
		}

		// Validate unique id:
		if (this.getShopkeeperByUniqueId(shopkeeper.getUniqueId()) != null) {
			throw new ShopkeeperCreateException("There is already a shopkeeper existing with this unique id: " + shopkeeper.getUniqueId());
		}

		// Success:
		shopkeeperStorage.onShopkeeperIdUsed(id);
		if (shopkeeper.isDirty()) shopkeeperStorage.markDirty();
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.CREATED);
		return shopkeeper;
	}

	@Override
	public AbstractShopkeeper loadShopkeeper(ShopType<?> shopType, int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		AbstractShopType<?> abstractShopType = this.validateShopType(shopType);
		Validate.notNull(configSection, "Missing config section!");
		Validate.isTrue(id >= 1, "Invalid id '" + id + "': Id has to be positive!");
		Validate.isTrue(this.getShopkeeperById(id) == null, "There is already a shopkeeper existing with this id: " + id);

		AbstractShopkeeper shopkeeper = abstractShopType.loadShopkeeper(id, configSection);
		if (shopkeeper == null) {
			// Invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' loaded null shopkeeper!");
		}

		// Validate unique id:
		if (this.getShopkeeperByUniqueId(shopkeeper.getUniqueId()) != null) {
			throw new ShopkeeperCreateException("There is already a shopkeeper existing with this unique id: " + shopkeeper.getUniqueId());
		}

		// Success:
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

	// ADD / REMOVE

	private void addShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperAddedEvent.Cause cause) {
		assert shopkeeper != null && !shopkeeper.isValid();
		assert !shopkeepersByUUID.containsKey(shopkeeper.getUniqueId());
		assert !shopkeepersById.containsKey(shopkeeper.getId());

		// Store by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.put(shopkeeperUniqueId, shopkeeper);
		shopkeepersById.put(shopkeeper.getId(), shopkeeper);

		ChunkCoords chunkCoords = shopkeeper.getChunkCoords(); // Null for virtual shops
		ChunkShopkeepers chunkEntry;
		if (chunkCoords == null) {
			// Virtual shopkeeper:
			chunkEntry = null;
			virtualShopkeepers.add(shopkeeper);
		} else {
			// Add shopkeeper to chunk:
			chunkEntry = this.addShopkeeperToChunk(shopkeeper, chunkCoords);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount++;
		}

		// Inform shopkeeper:
		shopkeeper.informAdded(cause);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperAddedEvent(shopkeeper, cause));

		// Activate shopkeeper:
		if (!shopkeeper.getShopObject().needsSpawning()) {
			// Activate shopkeeper once at registration:
			this._activateShopkeeper(shopkeeper);
		} else if (chunkEntry != null && chunkEntry.active) {
			if (!chunkEntry.worldEntry.isWorldSaveRespawnPending()) {
				// Spawn shopkeeper in active chunk:
				this.spawnShopkeeper(shopkeeper);
			} else {
				Log.debug(DebugOptions.shopkeeperActivation,
						() -> "Skipping spawning of shopkeeper at " + shopkeeper.getPositionString() + " due to pending respawn after world save."
				);
			}
		}
	}

	// Only called for non-virtual shopkeepers
	private ChunkShopkeepers addShopkeeperToChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		assert shopkeeper != null && chunkCoords != null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) {
			worldEntry = new WorldShopkeepers(worldName);
			shopkeepersByWorld.put(worldName, worldEntry);
		}
		return worldEntry.addShopkeeper(shopkeeper, chunkCoords); // Add to chunk
	}

	private void removeShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperRemoveEvent.Cause cause) {
		assert shopkeeper != null && shopkeeper.isValid() && cause != null;

		// Despawn shopkeeper:
		this.despawnShopkeeper(shopkeeper, true);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperRemoveEvent(shopkeeper, cause));

		// Inform shopkeeper:
		shopkeeper.informRemoval(cause);

		// Remove shopkeeper by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.remove(shopkeeperUniqueId);
		shopkeepersById.remove(shopkeeper.getId());

		ChunkCoords chunkCoords = shopkeeper.getChunkCoords(); // Null for virtual shops
		if (chunkCoords == null) {
			// Virtual shopkeeper:
			virtualShopkeepers.remove(shopkeeper);
		} else {
			// Remove shopkeeper from chunk:
			this.removeShopkeeperFromChunk(shopkeeper, chunkCoords);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount--;
		}

		if (cause == ShopkeeperRemoveEvent.Cause.DELETE) {
			// Remove shopkeeper from storage:
			this.getShopkeeperStorage().clearShopkeeperData(shopkeeper);
		}
	}

	// Only called for non-virtual shopkeepers.
	private void removeShopkeeperFromChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		assert shopkeeper != null && chunkCoords != null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // Could not find shopkeeper
		worldEntry.removeShopkeeper(shopkeeper, chunkCoords); // Remove from chunk
		if (worldEntry.shopkeeperCount <= 0) {
			worldEntry.cleanUp();
			shopkeepersByWorld.remove(worldName);
		}
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

	// This does not get called for virtual shopkeepers.
	public void onShopkeeperMove(AbstractShopkeeper shopkeeper, ChunkCoords oldChunk) {
		assert shopkeeper != null && oldChunk != null;
		ChunkCoords newChunk = shopkeeper.getChunkCoords();
		assert newChunk != null;
		if (!newChunk.equals(oldChunk)) {
			// Remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper, oldChunk);

			// Add to new chunk:
			this.addShopkeeperToChunk(shopkeeper, newChunk);
		}
	}

	// CHUNK ACTIVATION

	private ChunkShopkeepers getChunkEntry(ChunkCoords chunkCoords) {
		if (chunkCoords == null) return null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return null; // There are no shopkeepers in this world
		return worldEntry.shopkeepersByChunk.get(chunkCoords);
	}

	void onChunkLoad(Chunk chunk) {
		assert chunk != null;
		ChunkCoords chunkCoords = new ChunkCoords(chunk);
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return; // There are no shopkeepers in this chunk

		// Chunk is not expected to already be active or pending activation (if chunk loading and unloading events are
		// consistently ordered and correctly handled by us):
		if (chunkEntry.active) {
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "Detected chunk load for already active chunk: " + TextUtils.getChunkString(chunkCoords)
			);
			return;
		} else if (chunkEntry.isActivationPending()) {
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "Detected chunk load for already pending chunk activation: " + TextUtils.getChunkString(chunkCoords)
			);
			return;
		}

		// Defer activation to not activate shopkeepers for only briefly loaded chunks:
		chunkEntry.activationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			assert chunkCoords.isChunkLoaded(); // We stop the task on chunk unloads
			chunkEntry.activationTask = null;
			this.activateChunk(chunkEntry);
		}, CHUNK_ACTIVATION_DELAY_TICKS);
	}

	private void activateChunk(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null && chunkEntry.chunkCoords.isChunkLoaded();
		if (chunkEntry.active) { // Already active
			assert !chunkEntry.isActivationPending();
			return;
		}
		chunkEntry.cancelActivationTask(); // Stop pending activation if any
		chunkEntry.active = true; // Mark chunk active

		// Inform shopkeepers:
		for (AbstractShopkeeper shopkeeper : chunkEntry.shopkeepers) {
			shopkeeper.getShopObject().onChunkActivation();
		}

		// Spawn shopkeepers:
		this.spawnShopkeepers(chunkEntry, false);
	}

	private void spawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSavingFinished) {
		assert chunkEntry != null && chunkEntry.active;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		if (chunkEntry.worldEntry.isWorldSaveRespawnPending()) {
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "Skipping spawning of " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
							+ ": Respawn pending after world save."
			);
			return;
		}

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);

		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			// Spawn shopkeeper:
			this.spawnShopkeeper(shopkeeper);
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		if (dirty) {
			// Save delayed:
			plugin.getShopkeeperStorage().saveDelayed();
		}
	}

	// CHUNK DEACTIVATION

	void onChunkUnload(Chunk chunk) {
		assert chunk != null;
		ChunkCoords chunkCoords = new ChunkCoords(chunk);
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return; // There are no shopkeepers in this chunk

		this.deactivateChunk(chunkEntry);
	}

	private void deactivateChunk(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null;
		if (!chunkEntry.active) { // Already inactive
			chunkEntry.cancelActivationTask(); // Stop pending activation if any
			return;
		}
		assert !chunkEntry.isActivationPending();
		chunkEntry.active = false; // Mark chunk inactive

		// Inform shopkeepers:
		for (AbstractShopkeeper shopkeeper : chunkEntry.shopkeepers) {
			shopkeeper.getShopObject().onChunkDeactivation();
		}

		// Despawn shopkeepers:
		this.despawnShopkeepers(chunkEntry, false);
	}

	// Chunk might already be marked inactive when this is called.
	private void despawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSaving) {
		assert chunkEntry != null;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSaving ? " (world saving)" : "")
		);

		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			// Skip shopkeepers which are kept active all the time:
			if (!shopObject.needsSpawning()) continue;
			// If world save: Skip shopkeepers that do not need to be despawned.
			if (worldSaving && !shopObject.despawnDuringWorldSaves()) {
				continue;
			}

			// Despawn shopkeeper:
			this.despawnShopkeeper(shopkeeper, false);
		}
	}

	// WORLD LOAD

	// TODO This might not be needed, because chunk entries for loaded chunks get activated automatically when the first
	// shopkeeper gets added.
	public void activateShopkeepersInAllWorlds() {
		// Activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		for (World world : Bukkit.getWorlds()) {
			this.activateChunks(world);
		}
	}

	void onWorldLoad(World world) {
		assert world != null;
		this.activateChunks(world);
	}

	// Activates all loaded chunks of the given world.
	private void activateChunks(World world) {
		assert world != null;
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // There are no shopkeepers in this world

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldName + "'"
		);

		// Activate loaded chunks:
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			// Check if already active or activation pending (avoids unnecessary isChunkLoaded calls):
			if (chunkEntry.active || chunkEntry.isActivationPending()) {
				continue;
			}
			if (!chunkEntry.chunkCoords.isChunkLoaded()) {
				continue; // Chunk is not loaded
			}
			this.activateChunk(chunkEntry);
		}
	}

	// WORLD UNLOAD

	public void deactivateShopkeepersInAllWorlds() {
		// Deactivate (despawn) shopkeepers in all loaded worlds:
		for (World world : Bukkit.getWorlds()) {
			this.deactivateChunks(world);
		}
	}

	void onWorldUnload(World world) {
		assert world != null;
		this.deactivateChunks(world);
	}

	private void deactivateChunks(World world) {
		assert world != null;
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // There are no shopkeepers in this world

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldName + "'"
		);

		// Cancel world save respawn task:
		worldEntry.cancelWorldSaveRespawnTask();

		// Deactivate chunks:
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			this.deactivateChunk(chunkEntry);
		}
	}

	// WORLD SAVE

	void onWorldSave(World world) {
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // There are no shopkeepers in this world

		if (worldEntry.isWorldSaveRespawnPending()) {
			// Already despawned the shopkeepers due to another world save just recently.
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "Detected another world save while shopkeepers were already despawned due to a previous world save: " + worldName
			);
			return;
		}

		this.despawnShopkeepersInWorld(worldEntry, true);
		worldEntry.worldSaveRespawnTask = Bukkit.getScheduler().runTask(plugin, () -> {
			// Assert: World is still loaded and world entry is still valid (the task gets cancelled on world unload and
			// world entry cleanup).
			worldEntry.worldSaveRespawnTask = null;
			this.spawnShopkeepersInWorld(worldEntry, true);
		});
	}

	private void despawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSaving) {
		assert worldEntry != null;
		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSaving ? " (world saving)" : "")
		);
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			if (chunkEntry.active) {
				this.despawnShopkeepers(chunkEntry, worldSaving);
			}
		}
	}

	private void spawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSavingFinished) {
		assert worldEntry != null;
		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			if (chunkEntry.active) {
				this.spawnShopkeepers(chunkEntry, worldSavingFinished);
			}
		}
	}

	// SHOPKEEPER ACTIVATION

	private void spawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		ShopObject shopObject = shopkeeper.getShopObject();
		if (!shopObject.needsSpawning()) return;

		boolean activate = false;
		// Spawn if not yet active:
		if (!shopObject.isActive()) {
			// Deactivate shopkeeper by old shop object id, in case there is one:
			this._deactivateShopkeeper(shopkeeper);

			boolean spawned = shopObject.spawn();
			if (spawned) {
				// Activate with new object id:
				activate = true;
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		} else if (this.getActiveShopkeeper(shopObject.getId()) == null) {
			// Already active but missing activation, activate with current object id:
			activate = true;
		}
		if (activate) {
			// Activate with current object id:
			this._activateShopkeeper(shopkeeper);
		}
	}

	private void despawnShopkeeper(AbstractShopkeeper shopkeeper, boolean closeWindows) {
		assert shopkeeper != null;
		if (closeWindows) {
			// Delayed closing of all open windows:
			shopkeeper.abortUISessionsDelayed();
		}
		this._deactivateShopkeeper(shopkeeper);
		shopkeeper.getShopObject().despawn();
	}

	// Performs some validation before actually activating a shopkeeper.
	// Returns false if some validation failed.
	private boolean _activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getShopObject().getId(); // Current object id
		if (objectId == null) {
			// Currently only null is considered invalid.
			// Prints 'null' to log then:
			Log.warning("Detected shopkeeper with invalid object id: " + objectId);
			return false;
		} else if (activeShopkeepers.containsKey(objectId)) {
			Log.warning("Detected shopkeepers with duplicate object id: " + objectId);
			return false;
		} else {
			// Deactivate by old id in case there is one:
			this._deactivateShopkeeper(shopkeeper);
			assert shopkeeper.getShopObject().getLastId() == null;

			// Activate shopkeeper:
			activeShopkeepers.put(objectId, shopkeeper);
			shopkeeper.getShopObject().setLastId(objectId); // Remember object id
			return true;
		}
	}

	private boolean _deactivateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getShopObject().getLastId(); // Can be null
		if (objectId != null) {
			shopkeeper.getShopObject().setLastId(null);
			if (activeShopkeepers.get(objectId) == shopkeeper) {
				activeShopkeepers.remove(objectId);
				return true;
			}
		}
		return false;
	}

	// This can be used if the shopkeeper's object id has changed for some reason.
	public void onShopkeeperObjectIdChanged(AbstractShopkeeper shopkeeper) {
		// Deactivate by old (last) object id:
		this._deactivateShopkeeper(shopkeeper);
		// Re-activate by new (current) object id:
		this._activateShopkeeper(shopkeeper);
	}

	///// QUERYING

	@Override
	public Collection<? extends AbstractShopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getVirtualShopkeepers() {
		return virtualShopkeepersView;
	}

	@Override
	public AbstractShopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId) {
		return shopkeepersByUUID.get(shopkeeperUniqueId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperById(int shopkeeperId) {
		return shopkeepersById.get(shopkeeperId);
	}

	// PLAYER SHOPS

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getAllPlayerShopkeepers() {
		return allPlayerShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getPlayerShopkeepersByOwner(UUID ownerUUID) {
		Validate.notNull(ownerUUID, "Owner UUID is null!");
		// TODO Improve? Maybe keep an index of player shops? or even index by owner?
		// Note: Already unmodifiable.
		return new AbstractSet<AbstractPlayerShopkeeper>() {
			private Stream<AbstractPlayerShopkeeper> createStream() {
				return getAllShopkeepers().stream()
						.filter(shopkeeper -> shopkeeper instanceof PlayerShopkeeper)
						.map(shopkeeper -> (AbstractPlayerShopkeeper) shopkeeper)
						.filter(shopkeeper -> shopkeeper.getOwnerUUID().equals(ownerUUID));
			}

			@Override
			public Iterator<AbstractPlayerShopkeeper> iterator() {
				return this.createStream().iterator();
			}

			@Override
			public int size() {
				return this.createStream().mapToInt(shopkeeper -> 1).sum();
			}
		};
	}

	// BY NAME

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByName(String shopName) {
		String normalizedShopName = StringUtils.normalize(TextUtils.stripColor(shopName));
		if (StringUtils.isEmpty(normalizedShopName)) return Stream.empty();

		// TODO Improve via (Tree)Map?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // Can be empty
			if (shopkeeperName.isEmpty()) return false; // Has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			return (shopkeeperName.equals(normalizedShopName)); // Include shopkeeper if name matches
		});
	}

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByNamePrefix(String shopNamePrefix) {
		String normalizedShopNamePrefix = StringUtils.normalize(TextUtils.stripColor(shopNamePrefix));
		if (StringUtils.isEmpty(normalizedShopNamePrefix)) return Stream.empty();

		// TODO Improve via TreeMap?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // Can be empty
			if (shopkeeperName.isEmpty()) return false; // Has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			return (shopkeeperName.startsWith(normalizedShopNamePrefix)); // Include shopkeeper if name matches
		});
	}

	// BY WORLD

	@Override
	public Collection<String> getWorldsWithShopkeepers() {
		return shopkeeperWorldsView;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInWorld(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.shopkeepersView;
	}

	@Override
	public Map<ChunkCoords, ? extends Collection<? extends AbstractShopkeeper>> getShopkeepersByChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptyMap();
		return worldEntry.shopkeepersByChunkView;
	}

	// ACTIVE CHUNKS

	@Override
	public Collection<ChunkCoords> getActiveChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.activeChunksView;
	}

	@Override
	public boolean isChunkActive(ChunkCoords chunkCoords) {
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return false;
		return chunkEntry.active;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInActiveChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.shopkeepersInActiveChunksView;
	}

	// BY CHUNK

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords) {
		Validate.notNull(chunkCoords, "ChunkCoords is null!");
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		List<AbstractShopkeeper> shopkeepersInChunkView = worldEntry.shopkeepersByChunkView.get(chunkCoords);
		if (shopkeepersInChunkView == null) return Collections.emptySet();
		return shopkeepersInChunkView; // Unmodifiable already
	}

	// BY LOCATION

	public boolean isShopkeeperAtLocation(Location location) {
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");
		String worldName = world.getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		ChunkCoords chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
		for (AbstractShopkeeper shopkeeper : this.getShopkeepersInChunk(chunkCoords)) {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersAtLocation(Location location) {
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");
		String worldName = world.getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		ChunkCoords chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
		for (AbstractShopkeeper shopkeeper : this.getShopkeepersInChunk(chunkCoords)) {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				shopkeepers.add(shopkeeper);
			}
		}
		return shopkeepers;
	}

	// BY SHOP OBJECT

	@Override
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public AbstractShopkeeper getActiveShopkeeper(String objectId) {
		return activeShopkeepers.get(objectId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		// Check by default object id first:
		String objectId = DefaultEntityShopObjectIds.getObjectId(entity);
		AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
		if (shopkeeper != null) return shopkeeper;

		// Check for entity shop object types which use non-default object ids:
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractEntityShopObjectType) {
				AbstractEntityShopObjectType<?> entityShopObjectType = (AbstractEntityShopObjectType<?>) shopObjectType;
				if (entityShopObjectType.usesDefaultObjectIds()) continue;
				objectId = entityShopObjectType.createObjectId(entity);
				shopkeeper = this.getActiveShopkeeper(objectId);
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
		// Check by default object id first:
		String objectId = DefaultBlockShopObjectIds.getObjectId(block);
		AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
		if (shopkeeper != null) return shopkeeper;

		// Check for block shop object types which use non-default object ids:
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractBlockShopObjectType) {
				AbstractBlockShopObjectType<?> blockShopObjectType = (AbstractBlockShopObjectType<?>) shopObjectType;
				if (blockShopObjectType.usesDefaultObjectIds()) continue;
				objectId = blockShopObjectType.createObjectId(block);
				shopkeeper = this.getActiveShopkeeper(objectId);
				if (shopkeeper != null) return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeperByBlock(block) != null);
	}
}
