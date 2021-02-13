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
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.block.DefaultBlockShopObjectIds;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.entity.DefaultEntityShopObjectIds;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.CyclicCounter;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.MutableChunkCoords;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.taskqueue.TaskQueueStatistics;
import com.nisovin.shopkeepers.util.timer.Timer;
import com.nisovin.shopkeepers.util.timer.Timings;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	/**
	 * Spawning shopkeepers is relatively costly performance-wise. In order to not spawn shopkeepers for chunks that are
	 * only loaded briefly, we defer the activation of chunks by this amount of ticks. This also account for players who
	 * frequently cross chunk boundaries back and forth.
	 */
	private static final long CHUNK_ACTIVATION_DELAY_TICKS = 20;
	/**
	 * The radius in chunks around the player that we immediately activate when a player freshly joins, or teleports. A
	 * radius of {@code zero} only activates the player's own chunk.
	 * <p>
	 * The actually used radius is the minimum of this setting and the server's {@link Server#getViewDistance() view
	 * distance}.
	 */
	private static final int IMMEDIATE_CHUNK_ACTIVATION_RADIUS = 2;

	private static final Location sharedLocation = new Location(null, 0, 0, 0);
	private static final MutableChunkCoords sharedChunkCoords = new MutableChunkCoords();

	// TODO We assume that shopkeeper entities are stationary. If they get teleported into another chunk, or even
	// another world, we would need to check for them during chunk unloads, world unloads, and world saves (which we
	// currently don't).
	// This isn't that big of an issue, since all shopkeeper entities are non-persistent currently. But in general this
	// may be required.

	private static final class ChunkShopkeepers {

		final WorldShopkeepers worldEntry;
		final ChunkCoords chunkCoords;
		// List instead of set or map: We don't expect there to be excessive amounts of shopkeepers inside a single
		// chunk, so removal from the list should be sufficiently fast.
		final List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		// Note: The chunk stays marked as active during the temporary despawning of shopkeepers during world saves.
		boolean active;
		// TODO Use one task (or a small number of tasks) for all pending chunk activations, instead of one task per
		// chunk?
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
		final Map<ChunkCoords, ChunkShopkeepers> shopkeepersByChunk = new LinkedHashMap<>();
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
		// Shopkeepers in active chunks. Note: Already unmodifiable.
		final Set<AbstractShopkeeper> activeShopkeepersView = new AbstractSet<AbstractShopkeeper>() {
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

		ChunkShopkeepers addShopkeeper(AbstractShopkeeper shopkeeper) {
			assert shopkeeper != null;
			assert shopkeeper.getLastChunkCoords() == null;
			ChunkCoords chunkCoords = shopkeeper.getChunkCoords();
			assert chunkCoords != null;
			assert chunkCoords.getWorldName().equals(worldName);
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			if (chunkEntry == null) {
				// If the chunk is currently loaded, the chunk entry gets initialized as active:
				boolean chunkLoaded = chunkCoords.isChunkLoaded();
				chunkEntry = new ChunkShopkeepers(this, chunkCoords, chunkLoaded);
				shopkeepersByChunk.put(chunkCoords, chunkEntry);
				shopkeeperViewsByChunk.put(chunkCoords, Collections.unmodifiableList(chunkEntry.shopkeepers));
			}
			assert !chunkEntry.shopkeepers.contains(shopkeeper);
			chunkEntry.shopkeepers.add(shopkeeper);
			shopkeeper.setLastChunkCoords(chunkCoords);
			shopkeeperCount += 1;
			return chunkEntry;
		}

		ChunkShopkeepers removeShopkeeper(AbstractShopkeeper shopkeeper) {
			assert shopkeeper != null;
			ChunkCoords chunkCoords = shopkeeper.getLastChunkCoords();
			assert chunkCoords != null;
			assert chunkCoords.getWorldName().equals(worldName);
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			assert chunkEntry != null;
			assert chunkEntry.shopkeepers.contains(shopkeeper);
			chunkEntry.shopkeepers.remove(shopkeeper);
			shopkeeper.setLastChunkCoords(null);
			shopkeeperCount -= 1;
			if (chunkEntry.shopkeepers.isEmpty()) {
				chunkEntry.cleanUp();
				shopkeepersByChunk.remove(chunkCoords);
				shopkeeperViewsByChunk.remove(chunkCoords);
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

	// Note: This does not use the activeShopkeepersByObjectId map in order to be consistent with the active shopkeepers
	// queried by world. Chunks may get activated and deactivated during ticking, which is not reflected by the
	// activeShopkeepersByObjectId map.
	// Note: Already unmodifiable.
	final Set<AbstractShopkeeper> activeShopkeepersView = new AbstractSet<AbstractShopkeeper>() {
		@Override
		public Iterator<AbstractShopkeeper> iterator() {
			return shopkeepersByWorld.values().stream()
					.flatMap(worldShopkeepers -> worldShopkeepers.activeShopkeepersView.stream())
					.iterator();
		}

		@Override
		public int size() {
			return shopkeepersByWorld.values().stream()
					.mapToInt(worldShopkeepers -> worldShopkeepers.activeShopkeepersView.size())
					.sum();
		}
	};

	// The shopkeepers that are ticked (i.e. the (activated) shopkeepers in active chunks).
	// The shopkeepers are stored by their object id, or, if they don't provide one, by a shopkeeper-specific fallback
	// id (this ensures that shopkeepers that could not be spawned are still ticked, eg. for periodic respawn attempts).
	// Shopkeepers that are pending to be spawned (i.e. that are in the spawn queue) are not yet activated, and are
	// therefore also not yet ticked.
	private final Map<Object, AbstractShopkeeper> activeShopkeepersByObjectId = new LinkedHashMap<>();
	private boolean tickingShopkeepers = false;
	// True: Activate (or update previous activation, eg. after the object id changed)
	// False: Deactivate
	private final Map<AbstractShopkeeper, Boolean> pendingActivationChanges = new LinkedHashMap<>();

	private final Timer chunkActivationTimings = new Timer();
	private int immediateChunkActivationRadius;

	// A spawn queue which prevents that we spawn to many shopkeepers at the same time (which can lead to short
	// performance drops). Shopkeepers that are pending to be spawned are not yet activated, and are therefore also not
	// yet ticked until after the spawn queue has processed them.
	// In order to avoid players having to wait for shopkeepers to spawn, there are some situations in which we spawn
	// the shopkeepers immediately instead of adding them to the queue. This includes: When a shopkeeper is newly
	// created, when a shopkeeper is loaded (i.e. on plugin reloads), and after world saves. In the latter two cases, a
	// potentially large number of shopkeepers is expected to be spawned at the same time. Due to its limited
	// throughput, the queue would not be able to deal with this sudden peak appropriately. However, since these are
	// situations that are associated with a certain performance impact anyways, we prefer to spawn all the affected
	// shopkeepers immediately, instead of causing confusion among players by having them wait for the shopkeepers to
	// respawn.
	private final ShopkeeperSpawnQueue spawnQueue;

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.spawnQueue = new ShopkeeperSpawnQueue(plugin, this::spawnShopkeeper);
	}

	public void onEnable() {
		// Setup of static state related to shopkeepers:
		AbstractShopkeeper.setupOnEnable();

		// Determine the immediate chunk activation radius:
		immediateChunkActivationRadius = Math.min(IMMEDIATE_CHUNK_ACTIVATION_RADIUS, Bukkit.getViewDistance());

		// Start spawn queue:
		spawnQueue.start();

		// Start shopkeeper ticking task:
		this.startShopkeeperTickTask();

		Bukkit.getPluginManager().registerEvents(new WorldListener(this), plugin);
	}

	public void onDisable() {
		// Shutdown spawn queue (also clears the queue right away):
		spawnQueue.shutdown();

		// Unload all shopkeepers:
		this.unloadAllShopkeepers();
		assert this.getAllShopkeepers().isEmpty();

		// Reset, clearing (just in case):
		shopkeepersByUUID.clear();
		shopkeepersById.clear();
		shopkeepersByWorld.clear();
		virtualShopkeepers.clear();
		activeShopkeepersByObjectId.clear();
		playerShopCount = 0;
		chunkActivationTimings.reset();
	}

	// TIMINGS

	public Timings getChunkActivationTimings() {
		return chunkActivationTimings;
	}

	// TICKING

	private class ShopkeeperTickTask extends BukkitRunnable {

		private final CyclicCounter tickingGroup = new CyclicCounter(AbstractShopkeeper.TICKING_GROUPS);
		private boolean dirty;
		private boolean visualizeTicks;

		public void start() {
			// For load balancing purposes, we run the task more often and then process only a subset of all active
			// shopkeepers:
			int period = AbstractShopkeeper.TICKING_PERIOD_TICKS / AbstractShopkeeper.TICKING_GROUPS;
			this.runTaskTimer(plugin, period, period);
		}

		@Override
		public void run() {
			dirty = false;
			// We only check once per shopkeeper tick if the tick visualization is enabled (by default this avoids that
			// each shopkeeper and shop object has to check this itself):
			// TODO We could probably also cache this once on plugin enable and only update it on changes to the
			// settings. However, this check isn't actually that costly that this would be required.
			visualizeTicks = Debug.isDebugging(DebugOptions.visualizeShopkeeperTicks);

			tickingShopkeepers = true;
			activeShopkeepersByObjectId.values().forEach(this::tickShopkeeper);
			tickingShopkeepers = false;

			// Process pending shopkeeper activation changes (includes shopkeepers whose object ids have changed):
			pendingActivationChanges.forEach((shopkeeper, activate) -> {
				if (activate) {
					activateShopkeeper(shopkeeper);
				} else {
					deactivateShopkeeper(shopkeeper);
				}
			});
			pendingActivationChanges.clear();

			// Trigger a delayed save if any of the shopkeepers got marked as dirty:
			if (dirty) {
				getShopkeeperStorage().saveDelayed();
			}

			// Update ticking group:
			tickingGroup.getAndIncrement();
		}

		private void tickShopkeeper(AbstractShopkeeper shopkeeper) {
			assert shopkeeper.getShopObject().getLastId() != null; // We only tick the active shopkeepers
			if (shopkeeper.getTickingGroup() != tickingGroup.getValue()) return;
			// Skip if the shopkeeper is no longer valid (got deleted) or is pending deactivation.
			// Note: Checking if the shopkeeper is pending deactivation is enough, since deleting the shopkeeper also
			// deactivates it.
			if (isPendingDeactivation(shopkeeper)) return;

			// Tick shopkeeper:
			shopkeeper.tick();

			// Skip the ticking of the shop object if the shopkeeper got deleted or deactivated during the tick:
			boolean tickShopObject = !isPendingDeactivation(shopkeeper);

			// Tick shop object:
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			if (tickShopObject) {
				shopObject.tick();

				// If the shop object had to be respawned, its id might have changed.
				if (!shopObject.getLastId().equals(shopObject.getId())) {
					onShopkeeperObjectIdChanged(shopkeeper);
				}
			}

			if (visualizeTicks) {
				shopkeeper.visualizeLastTick();
				if (tickShopObject) {
					shopObject.visualizeLastTick();
				}
			}

			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}
	}

	private void startShopkeeperTickTask() {
		new ShopkeeperTickTask().start();
	}

	// This can be used during shopkeeper ticking to check if the shopkeeper is pending deactivation. This does not
	// check if the shopkeeper is currently active.
	private boolean isPendingDeactivation(AbstractShopkeeper shopkeeper) {
		return Boolean.FALSE.equals(pendingActivationChanges.get(shopkeeper));
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

		// Add the shopkeeper to the registry and spawn it:
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.CREATED);
		return shopkeeper;
	}

	/**
	 * Recreates a shopkeeper by loading its previously saved data from the given config section.
	 * 
	 * @param shopType
	 *            the shop type
	 * @param id
	 *            the shopkeeper id
	 * @param configSection
	 *            the config section to load the shopkeeper data from
	 * @return the loaded shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	// Internal method: This is only supposed to be called by the built-in storage currently. If the data comes from any
	// other source, the storage would need to be made aware of the shopkeeper (eg. by marking the shopkeeper as dirty).
	// Otherwise, certain operations (such as checking if a certain shopkeeper id is already in use) would no longer
	// work as expected.
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

		// Add the shopkeeper to the registry and spawn it:
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
		int shopkeeperId = shopkeeper.getId();
		shopkeepersByUUID.put(shopkeeperUniqueId, shopkeeper);
		shopkeepersById.put(shopkeeperId, shopkeeper);

		// Inform the storage about the used up id:
		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		shopkeeperStorage.onShopkeeperIdUsed(shopkeeperId);

		ChunkShopkeepers chunkEntry;
		if (shopkeeper.isVirtual()) {
			// Virtual shopkeeper:
			chunkEntry = null;
			virtualShopkeepers.add(shopkeeper);
		} else {
			// Add shopkeeper to chunk:
			chunkEntry = this.addShopkeeperToChunk(shopkeeper);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount++;
		}

		// Log a warning if either the shop type or the shop object type is disabled. The shopkeeper is still added (so
		// containers are still protected), but it might not get spawned, and there is no guarantee that the shop still
		// works as expected. Admins are advised to either delete the shopkeeper, or change its object type to something
		// else.
		AbstractShopType<?> shopType = shopkeeper.getType();
		if (!shopType.isEnabled()) {
			Log.warning("Shop type '" + shopType.getIdentifier() + "' of shopkeeper " + shopkeeper.getId()
					+ " is disabled! Consider deleting this shopkeeper.");
		}
		AbstractShopObjectType<?> shopObjectType = shopkeeper.getShopObject().getType();
		if (!shopObjectType.isEnabled()) {
			Log.warning("Object type '" + shopObjectType.getIdentifier() + "' of shopkeeper " + shopkeeper.getId()
					+ " is disabled! Consider changing its object type.");
		}

		// Inform shopkeeper:
		shopkeeper.informAdded(cause);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperAddedEvent(shopkeeper, cause));
		if (!shopkeeper.isValid()) {
			// The shopkeeper has been removed again:
			return;
		}

		// Activate the shopkeeper if the chunk is currently active:
		if (chunkEntry != null && chunkEntry.active) {
			if (shopObjectType.mustBeSpawned()) {
				if (chunkEntry.worldEntry.isWorldSaveRespawnPending() && shopObjectType.mustDespawnDuringWorldSave()) {
					Log.debug(DebugOptions.shopkeeperActivation,
							() -> "Skipping spawning of shopkeeper at " + shopkeeper.getPositionString() + " due to pending respawn after world save."
					);
				} else {
					// Spawn and activate the shopkeeper:
					// In order to not have players wait for newly created shopkeepers, as well as for loaded
					// shopkeepers after plugin reloads, we don't use the spawn queue here, but spawn the shopkeeper
					// immediately.
					this.spawnShopkeeper(shopkeeper);
				}
			} else {
				// Only activate the shopkeeper. Its shop object handles spawning itself:
				this.activateShopkeeper(shopkeeper);
			}
		}
	}

	// Only called for non-virtual shopkeepers
	private ChunkShopkeepers addShopkeeperToChunk(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		assert shopkeeper.getLastChunkCoords() == null;
		String worldName = shopkeeper.getWorldName();
		assert worldName != null;
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) {
			worldEntry = new WorldShopkeepers(worldName);
			shopkeepersByWorld.put(worldName, worldEntry);
		}
		return worldEntry.addShopkeeper(shopkeeper);
	}

	private void removeShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperRemoveEvent.Cause cause) {
		assert shopkeeper != null && shopkeeper.isValid() && cause != null;

		// Delayed closing of all open windows:
		shopkeeper.abortUISessionsDelayed();

		// Despawn and deactivate shopkeeper:
		if (shopkeeper.getShopObject().getType().mustBeSpawned()) {
			this.despawnShopkeeper(shopkeeper);
		} else {
			// Only deactivate the shopkeeper:
			this.deactivateShopkeeper(shopkeeper);
		}

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperRemoveEvent(shopkeeper, cause));

		// Inform shopkeeper:
		shopkeeper.informRemoval(cause);

		// Remove shopkeeper by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.remove(shopkeeperUniqueId);
		shopkeepersById.remove(shopkeeper.getId());

		if (shopkeeper.isVirtual()) {
			// Virtual shopkeeper:
			virtualShopkeepers.remove(shopkeeper);
		} else {
			// Remove shopkeeper from chunk:
			this.removeShopkeeperFromChunk(shopkeeper);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount--;
		}

		if (cause == ShopkeeperRemoveEvent.Cause.DELETE) {
			// Remove shopkeeper from storage:
			this.getShopkeeperStorage().deleteShopkeeper(shopkeeper);
		}
	}

	// Only called for non-virtual shopkeepers.
	private void removeShopkeeperFromChunk(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String worldName = shopkeeper.getWorldName();
		assert worldName != null;
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // Could not find shopkeeper
		worldEntry.removeShopkeeper(shopkeeper); // Remove from chunk
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
		new ArrayList<>(this.getAllShopkeepers()).forEach(this::unloadShopkeeper);
	}

	public void deleteShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "Shopkeeper is null!");
		Validate.isTrue(shopkeeper.isValid(), "Shopkeeper is invalid!");
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.DELETE);
	}

	public void deleteAllShopkeepers() {
		new ArrayList<>(this.getAllShopkeepers()).forEach(this::deleteShopkeeper);
	}

	// This does not get called for virtual shopkeepers.
	public void onShopkeeperMoved(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null && !shopkeeper.isVirtual();
		ChunkCoords oldChunk = shopkeeper.getLastChunkCoords();
		ChunkCoords newChunk = shopkeeper.getChunkCoords();
		assert oldChunk != null && newChunk != null;
		if (!newChunk.equals(oldChunk)) {
			// Remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper);

			// Add to new chunk:
			this.addShopkeeperToChunk(shopkeeper);
		}
	}

	// CHUNK ACTIVATION

	private ChunkShopkeepers getChunkEntry(ChunkCoords chunkCoords) {
		if (chunkCoords == null) return null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return null; // There are no shopkeepers in this world
		// Returns null if there are no shopkeepers in this chunk:
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
		new ActivateChunkTask(chunkEntry).start();
	}

	private class ActivateChunkTask implements Runnable {

		private final ChunkShopkeepers chunkShopkeepers;

		public ActivateChunkTask(ChunkShopkeepers chunkShopkeepers) {
			assert chunkShopkeepers != null;
			this.chunkShopkeepers = chunkShopkeepers;
		}

		public void start() {
			assert chunkShopkeepers.activationTask == null;
			chunkShopkeepers.activationTask = Bukkit.getScheduler().runTaskLater(plugin, this, CHUNK_ACTIVATION_DELAY_TICKS);
		}

		@Override
		public void run() {
			assert chunkShopkeepers.chunkCoords.isChunkLoaded(); // We stop the task on chunk unloads
			chunkShopkeepers.activationTask = null;
			activateChunk(chunkShopkeepers);
		}
	}

	void activatePendingNearbyChunksDelayed(Player player) {
		assert player != null;
		Bukkit.getScheduler().runTask(plugin, new ActivatePendingNearbyChunksTask(player));
	}

	private class ActivatePendingNearbyChunksTask implements Runnable {

		private final Player player;

		ActivatePendingNearbyChunksTask(Player player) {
			assert player != null;
			this.player = player;
		}

		@Override
		public void run() {
			if (!player.isOnline()) return; // Player is no longer online
			activatePendingNearbyChunks(player);
		}
	}

	// Activates nearby chunks if they are currently pending activation:
	private void activatePendingNearbyChunks(Player player) {
		World world = player.getWorld();
		Location location = player.getLocation(sharedLocation);
		int chunkX = ChunkCoords.fromBlock(location.getBlockX());
		int chunkZ = ChunkCoords.fromBlock(location.getBlockZ());
		sharedLocation.setWorld(null); // Reset
		this.activatePendingNearbyChunks(world, chunkX, chunkZ, immediateChunkActivationRadius);
	}

	// Activates nearby chunks if they are currently pending activation:
	private void activatePendingNearbyChunks(World world, int centerChunkX, int centerChunkZ, int chunkRadius) {
		assert world != null && chunkRadius >= 0;
		String worldName = world.getName();
		int minChunkX = centerChunkX - chunkRadius;
		int maxChunkX = centerChunkX + chunkRadius;
		int minChunkZ = centerChunkZ - chunkRadius;
		int maxChunkZ = centerChunkZ + chunkRadius;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				sharedChunkCoords.set(worldName, chunkX, chunkZ);
				ChunkShopkeepers chunkEntry = this.getChunkEntry(sharedChunkCoords);
				if (chunkEntry == null) continue;

				// Activate the chunk if it is currently pending activation:
				if (chunkEntry.isActivationPending()) {
					this.activateChunk(chunkEntry);
				}
			}
		}
	}

	private void activateChunkIfLoaded(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null;
		// Check if already active or activation pending (avoids unnecessary isChunkLoaded calls):
		if (chunkEntry.active || chunkEntry.isActivationPending()) {
			return;
		}
		if (!chunkEntry.chunkCoords.isChunkLoaded()) {
			return; // Chunk is not loaded
		}
		this.activateChunk(chunkEntry);
	}

	private void activateChunk(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null && chunkEntry.chunkCoords.isChunkLoaded();
		if (chunkEntry.active) { // Already active
			assert !chunkEntry.isActivationPending();
			return;
		}

		chunkActivationTimings.start();

		chunkEntry.cancelActivationTask(); // Stop pending activation if any
		chunkEntry.active = true; // Mark chunk active

		// Inform shopkeepers:
		chunkEntry.shopkeepers.forEach(this::informShopkeeperOnChunkActivation);

		// Spawn shopkeepers:
		this.spawnShopkeepers(chunkEntry, false);

		chunkActivationTimings.stop();
	}

	private void informShopkeeperOnChunkActivation(AbstractShopkeeper shopkeeper) {
		shopkeeper.onChunkActivation();
		shopkeeper.getShopObject().onChunkActivation();
	}

	// This also activates the shopkeepers, unless the world was saved and the shopkeeper has been kept spawned.
	private void spawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSavingFinished) {
		assert chunkEntry != null && chunkEntry.active;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);

		boolean worldSaveRespawnPending = chunkEntry.worldEntry.isWorldSaveRespawnPending();
		int spawned = 0;
		int awaitingWorldSaveRespawn = 0;
		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();
			boolean activate = true; // Whether to activate the shopkeeper
			// Only consider shopkeepers that are dynamically spawned and despawned by us:
			if (objectType.mustBeSpawned()) {
				if (worldSaveRespawnPending && objectType.mustDespawnDuringWorldSave()) {
					// Skip due to pending world save respawn:
					awaitingWorldSaveRespawn++;
					continue;
				}

				// If world saving finished, only consider shopkeepers that need to be respawned:
				if (!worldSavingFinished || objectType.mustDespawnDuringWorldSave()) {
					spawned++;
					if (worldSavingFinished) {
						// In order to not have players wait for shopkeepers to respawn after world saves, we respawn
						// the shopkeepers immediately:
						this.spawnShopkeeper(shopkeeper);
					} else {
						// In order to avoid spawning lots of shopkeepers at the same time, we don't actually spawn the
						// shopkeeper here, but add it to the spawn queue:
						spawnQueue.add(shopkeeper);
					}
				}
				// The shopkeeper either got spawned (and thereby already activated), or it has been kept spawned during
				// the world save and therefore does not need to be reactivated:
				activate = false;
			} else if (worldSavingFinished) {
				// The shop object takes care of spawning and despawning itself, and is not processed by us during world
				// saves:
				activate = false;
			}

			// Activate the shopkeeper if necessary:
			if (activate) {
				this.activateShopkeeper(shopkeeper);
			}

			// Check if the shopkeeper is dirty:
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		int actuallySpawned = spawned;
		Log.debug(DebugOptions.shopkeeperActivation, () -> "  Actually spawned: " + actuallySpawned + (worldSavingFinished ? "" : " (queued)"));

		if (awaitingWorldSaveRespawn > 0) {
			int skipped = awaitingWorldSaveRespawn;
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "  Skipped due to a pending respawn after world save: " + skipped
			);
		}

		if (dirty) {
			// Save delayed:
			this.getShopkeeperStorage().saveDelayed();
		}
	}

	public TaskQueueStatistics getSpawnQueueStatistics() {
		return spawnQueue;
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
		chunkEntry.shopkeepers.forEach(this::informShopkeeperOnChunkDeactivation);

		// Despawn and deactivate shopkeepers:
		this.despawnShopkeepers(chunkEntry, false);
	}

	private void informShopkeeperOnChunkDeactivation(AbstractShopkeeper shopkeeper) {
		shopkeeper.onChunkDeactivation();
		shopkeeper.getShopObject().onChunkDeactivation();
	}

	// The chunk might already be marked as inactive when this is called.
	// This also deactivates the shopkeepers, unless the world is being saved and the shopkeeper is kept spawned.
	private void despawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSaving) {
		assert chunkEntry != null;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSaving ? " (world saving)" : "")
		);

		int despawned = 0;
		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();
			boolean deactivate = true; // Whether to deactivate the shopkeeper
			// Only consider shopkeepers that are dynamically spawned and despawned by us:
			if (objectType.mustBeSpawned()) {
				// If world saving, only consider shopkeepers that need to be despawned:
				if (!worldSaving || objectType.mustDespawnDuringWorldSave()) {
					despawned++;
					this.despawnShopkeeper(shopkeeper);
				}
				// The shopkeeper either got despawned (and thereby already deactivated), or it is kept spawned and
				// therefore active:
				deactivate = false;
			} else if (worldSaving) {
				// The shop object takes care of spawning and despawning itself, and is not processed by us during world
				// saves:
				deactivate = false;
			}

			// Deactivate the shopkeeper if necessary:
			if (deactivate) {
				this.deactivateShopkeeper(shopkeeper);
			}

			// Check if the shopkeeper is dirty:
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		int actuallyDespawned = despawned;
		Log.debug(DebugOptions.shopkeeperActivation, () -> "  Actually despawned: " + actuallyDespawned);

		if (dirty) {
			// Save delayed:
			this.getShopkeeperStorage().saveDelayed();
		}
	}

	// WORLD LOAD

	// TODO This might not be needed, because chunk entries for loaded chunks get activated automatically when the first
	// shopkeeper gets added.
	public void activateShopkeepersInAllWorlds() {
		// Activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		Bukkit.getWorlds().forEach(this::activateChunks);
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
		worldEntry.shopkeepersByChunk.values().forEach(this::activateChunkIfLoaded);
	}

	// WORLD UNLOAD

	public void deactivateShopkeepersInAllWorlds() {
		// Deactivate (despawn) shopkeepers in all loaded worlds:
		Bukkit.getWorlds().forEach(this::deactivateChunks);
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
		worldEntry.shopkeepersByChunk.values().forEach(this::deactivateChunk);
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
		new RespawnShopkeepersAfterWorldSaveTask(worldEntry).start();
	}

	private class RespawnShopkeepersAfterWorldSaveTask implements Runnable {

		private final WorldShopkeepers worldShopkeepers;

		public RespawnShopkeepersAfterWorldSaveTask(WorldShopkeepers worldShopkeepers) {
			assert worldShopkeepers != null;
			this.worldShopkeepers = worldShopkeepers;
		}

		public void start() {
			assert worldShopkeepers.worldSaveRespawnTask == null;
			worldShopkeepers.worldSaveRespawnTask = Bukkit.getScheduler().runTask(plugin, this);
		}

		@Override
		public void run() {
			// Assert: World is still loaded and world entry is still valid (the task gets cancelled on world unload and
			// world entry cleanup).
			worldShopkeepers.worldSaveRespawnTask = null;
			spawnShopkeepersInWorld(worldShopkeepers, true);
		}
	}

	private void despawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSaving) {
		assert worldEntry != null;
		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSaving ? " (world saving)" : "")
		);
		worldEntry.shopkeepersByChunk.values().forEach(chunkEntry -> {
			if (chunkEntry.active) {
				this.despawnShopkeepers(chunkEntry, worldSaving);
			}
		});
	}

	private void spawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSavingFinished) {
		assert worldEntry != null;
		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);
		worldEntry.shopkeepersByChunk.values().forEach(chunkEntry -> {
			if (chunkEntry.active) {
				this.spawnShopkeepers(chunkEntry, worldSavingFinished);
			}
		});
	}

	// SHOPKEEPER ACTIVATION

	// Also activates the shopkeeper.
	private void spawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		AbstractShopObjectType<?> shopObjectType = shopObject.getType();
		assert shopObjectType.mustBeSpawned();
		if (shopObjectType.isEnabled()) {
			boolean spawned = shopObject.spawn();
			if (spawned) {
				// Validation:
				Object objectId = shopObject.getId();
				if (objectId == null) {
					Log.warning("Shopkeeper " + shopkeeper.getId() + " has been spawned but provides no object id!");
				}
			} else {
				Log.warning("Failed to spawn shopkeeper " + shopkeeper.getId() + " at " + shopkeeper.getPositionString());
			}
		} else {
			Log.debug(DebugOptions.shopkeeperActivation, () -> "Skipping spawning of shopkeeper " + shopkeeper.getId()
					+ ": Object type '" + shopObjectType.getIdentifier() + "' is disabled.");
		}

		// In either case, activate the shopkeeper:
		this.activateShopkeeper(shopkeeper);
	}

	// This also deactivates the shopkeeper.
	private void despawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		spawnQueue.remove(shopkeeper);
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		assert shopObject.getType().mustBeSpawned();
		shopObject.despawn();
		this.deactivateShopkeeper(shopkeeper);
	}

	// The object id that is used for shopkeepers in active chunks that could not be spawned or report to not be active:
	private Object getInactiveShopObjectId(AbstractShopkeeper shopkeeper) {
		// The shopkeeper UUID should be globally unique and therefore not conflict with object ids of other
		// shopkeepers:
		return shopkeeper.getUniqueId();
	}

	// If the shop object provides no id (eg. if it is not actually active), we store the shopkeeper under a fallback id
	// nevertheless. This allows us to keep track of all shopkeepers that are located in active chunks (eg. in order to
	// still tick them, so that they can check if they should be respawned).
	// If the shopkeeper is already active under a different object id, that entry is removed first.
	private void activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (tickingShopkeepers) {
			// Defer activation until after ticking:
			pendingActivationChanges.put(shopkeeper, true); // Replaces any previous value for the shopkeeper
			return;
		}

		AbstractShopObject shopObject = shopkeeper.getShopObject();

		// Deactivate the shopkeeper by its old id (if there is one):
		this.deactivateShopkeeper(shopkeeper);
		assert shopObject.getLastId() == null;

		// Get the new object id:
		Object objectId = shopObject.getId(); // Can be null if the shop object is not active
		if (objectId == null) {
			// We store the shopkeeper under its inactive object id:
			objectId = this.getInactiveShopObjectId(shopkeeper);
		}
		assert objectId != null;

		AbstractShopkeeper activeShopkeeper = this.getActiveShopkeeper(objectId);
		if (activeShopkeeper != null) {
			Log.warning("Detected shopkeepers (" + activeShopkeeper.getId() + " and " + shopkeeper.getId()
					+ ") with duplicate object ids: " + objectId);
			return;
		}

		// Activate shopkeeper:
		activeShopkeepersByObjectId.put(objectId, shopkeeper);
		shopObject.setLastId(objectId); // Remember object id
	}

	// Removes the shopkeeper from the active shopkeepers by its last shop object id (if there is one).
	private void deactivateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		Object objectId = shopObject.getLastId(); // Can be null
		if (objectId == null) return; // Already inactive

		if (tickingShopkeepers) {
			// Defer deactivation until after ticking:
			pendingActivationChanges.put(shopkeeper, false); // Replaces any previous value for the shopkeeper
			return;
		}

		assert activeShopkeepersByObjectId.get(objectId) == shopkeeper;
		activeShopkeepersByObjectId.remove(objectId);
		shopObject.setLastId(null);
	}

	// Updates the shopkeeper's entry in the active shopkeepers.
	// This can be used if the shopkeeper's object id has changed for some reason.
	// This is not required to be called if the object id changes during spawning, despawning, or ticking.
	public void onShopkeeperObjectIdChanged(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (shopkeeper.getShopObject().getLastId() == null) {
			// The shopkeeper has no entry in the active shopkeepers currently that would need to be updated.
			return;
		}
		if (this.isPendingDeactivation(shopkeeper)) {
			// The shopkeeper is pending deactivation, so there is no need to update its entry.
			return;
		}

		// Deactivate by old object id and activate by new object id (or fallback id):
		this.activateShopkeeper(shopkeeper);
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
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.activeShopkeepersView;
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

		ChunkCoords chunkCoords = ChunkCoords.fromBlock(worldName, x, z);
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
		ChunkCoords chunkCoords = ChunkCoords.fromBlock(worldName, x, z);
		for (AbstractShopkeeper shopkeeper : this.getShopkeepersInChunk(chunkCoords)) {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				shopkeepers.add(shopkeeper);
			}
		}
		return shopkeepers;
	}

	// BY SHOP OBJECT

	public AbstractShopkeeper getActiveShopkeeper(Object objectId) {
		return activeShopkeepersByObjectId.get(objectId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		// Check by default object id first:
		Object objectId = DefaultEntityShopObjectIds.getObjectId(entity);
		AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
		if (shopkeeper != null) return shopkeeper;

		// Check for entity shop object types which use non-default object ids:
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractEntityShopObjectType) {
				AbstractEntityShopObjectType<?> entityShopObjectType = (AbstractEntityShopObjectType<?>) shopObjectType;
				// TODO Cache the shop object types that don't use default ids.
				if (entityShopObjectType.usesDefaultObjectIds()) continue;
				objectId = entityShopObjectType.getObjectId(entity);
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
		Object objectId = DefaultBlockShopObjectIds.getObjectId(block);
		AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
		if (shopkeeper != null) return shopkeeper;

		// Check for block shop object types which use non-default object ids:
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractBlockShopObjectType) {
				AbstractBlockShopObjectType<?> blockShopObjectType = (AbstractBlockShopObjectType<?>) shopObjectType;
				// TODO Cache the shop object types that don't use default ids.
				if (blockShopObjectType.usesDefaultObjectIds()) continue;
				objectId = blockShopObjectType.getObjectId(block);
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
