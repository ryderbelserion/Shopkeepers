package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.util.ChunkCoords;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Handles gravity and look-at-nearby-players behavior.
 * <p>
 * It is assumed that entities usually don't change their initial chunk: Their gravity and AI activation depend on
 * whether their initial chunk has players nearby, rather than whether their current chunk has players nearby.
 */
public class LivingEntityAI {

	// The look-at-players ai goal only targets players in 12 block radius, so we can limit the ai ticking to the direct
	// chunks around the player:
	private static final int AI_ACTIVATION_CHUNK_RANGE = 1;
	// Regarding gravity activation range:
	// Players can see shop entities from further away, so we use a large enough range for the activation of falling
	// checks (configurable in the config, default 4)
	// TODO take view/tracking distances into account? (spigot-config specific though..)
	// TODO visual glitch if a player is near the tracking range: SPIGOT-3948

	// entities won't fall, if their distance-to-ground is smaller than this
	private static final double DISTANCE_TO_GROUND_THRESHOLD = 0.01D;
	// determines the max. falling speed:
	// note: we allow a falling step size that is slightly larger than this, if we reach the end of the fall by that
	// note: entities get spawned 0.5 above the ground
	// by using 0.5 here (and allowing slightly larger step sizes if they stop the fall) we can be sure to require at
	// most a single step for the most common falls and have the entity positioned perfectly on the ground
	// TODO dynamically increase an entities falling speed? need to dynamically adjust the collision check range as well
	// then
	private static final double MAX_FALLING_DISTANCE_PER_TICK = 0.5D;
	// the range in which we check for block collisions:
	// has to be slightly larger than the max-falling-distance-per-tick in order to make full use of the max. falling
	// speed, and to be able to detect the end of the falling without having to check for block collisions another time
	// in the next tick
	private static final double GRAVITY_COLLISION_CHECK_RANGE = MAX_FALLING_DISTANCE_PER_TICK + 0.1D;

	private static class EntityData {
		private final ChunkData chunkData;
		public boolean falling;
		public double distanceToGround = 0.0D;

		public EntityData(ChunkData chunkData, boolean activeGravity) {
			this.chunkData = chunkData;
			// active by default to quickly check for falling conditions initially:
			this.falling = activeGravity;
		}
	}

	// ticking entities -> entity data
	private final Map<LivingEntity, EntityData> entities = new HashMap<>();

	private static class ChunkData {
		private final Chunk chunk;
		private int entityCount = 0;
		// active by default for fast initial reactions in case players are nearby:
		public boolean activeGravity;
		public boolean activeAI = true;

		public ChunkData(Chunk chunk, boolean activeGravity) {
			this.chunk = chunk;
			this.activeGravity = activeGravity;
		}
	}

	private final Map<Chunk, ChunkData> activeChunks = new HashMap<>();

	// temporarily re-used Location object:
	private final Location tempLocation = new Location(null, 0, 0, 0);

	private BukkitTask aiTask = null;
	private boolean currentlyRunning = false;
	private int tickCounter = 0;

	// statistics:
	private int activeAIChunksCount = 0;
	private int activeAIEntityCount = 0;

	private int activeGravityChunksCount = 0;
	private int activeGravityEntityCount = 0;

	public static class Timings {

		private long[] timingsHistory;
		private long maxTiming = 0L;
		private int counter = 0;

		// current timing:
		private boolean started = false;
		private boolean paused = false;
		private long startTime;
		private long elapsedTime;

		public Timings() {
			this(100);
		}

		public Timings(int historySize) {
			assert historySize > 0;
			timingsHistory = new long[historySize];
		}

		void start() {
			assert !started && !paused;
			// reset:
			started = true;
			paused = false;
			elapsedTime = 0L;
			// start timing:
			startTime = System.nanoTime();
		}

		void startPaused() {
			this.start();
			this.pause();
		}

		void pause() {
			assert started && !paused;
			paused = true;
			// update timing:
			elapsedTime += (System.nanoTime() - startTime);
		}

		void resume() {
			assert started && paused;
			paused = false;
			// continue timing:
			startTime = System.nanoTime();
		}

		void stop() {
			assert started;
			counter++;
			if (!paused) {
				// update timing by pausing:
				this.pause();
			}
			assert paused;

			// update timings history:
			int historyIndex = (counter % timingsHistory.length);
			timingsHistory[historyIndex] = elapsedTime;
			// reset/update max timing:
			if (historyIndex == 0) maxTiming = elapsedTime;
			else if (elapsedTime > maxTiming) maxTiming = elapsedTime;
		}

		public void reset() {
			counter = 0;
			Arrays.fill(timingsHistory, 0L);
			maxTiming = 0L;
		}

		public int getCounter() {
			return counter;
		}

		public double getAverageTimeMillis() {
			return (Utils.average(timingsHistory) * 1.0E-6D);
		}

		public double getMaxTimeMillis() {
			return (maxTiming * 1.0E-6D);
		}
	}

	private final Timings totalTimings = new Timings();
	private final Timings activationTimings = new Timings(10);
	private final Timings gravityTimings = new Timings();
	private final Timings aiTimings = new Timings();

	public LivingEntityAI() {
	}

	// whether our custom gravity handling shall be active
	private boolean isGravityActive() {
		// gravity is enabled and not already handled by minecraft itself:
		return !Settings.disableGravity && NMSManager.getProvider().isNoAIDisablingGravity();
	}

	public void start() {
		if (this.isActive()) return;
		else if (aiTask != null) this.stop(); // not active, but already setup: perform cleanup

		// start ai task:
		aiTask = Bukkit.getScheduler().runTaskTimer(ShopkeepersPlugin.getInstance(), () -> {
			currentlyRunning = true;
			tickCounter++;

			// start timings:
			totalTimings.start();
			gravityTimings.startPaused();
			aiTimings.startPaused();

			// freshly determine active chunks/entities (near players) every 20 ticks:
			boolean activationPhase = (tickCounter % 20 == 0);
			if (activationPhase) {
				activationTimings.start();

				// deactivate all chunks:
				for (ChunkData chunkData : activeChunks.values()) {
					chunkData.activeAI = false;
					chunkData.activeGravity = false;
				}
				activeAIChunksCount = 0;
				activeGravityChunksCount = 0;

				// activate chunks with nearby players:
				// active gravity handling?
				boolean gravityActive = this.isGravityActive();
				int gravityChunkRange = Math.max(Settings.gravityChunkRange, 0);
				for (Player player : Bukkit.getOnlinePlayers()) {
					Chunk centerChunk = player.getLocation(tempLocation).getChunk();
					this.activateNearbyChunks(centerChunk, AI_ACTIVATION_CHUNK_RANGE, ActivationType.AI);
					if (gravityActive) {
						this.activateNearbyChunks(centerChunk, gravityChunkRange, ActivationType.GRAVITY);
					}
				}
				activationTimings.stop();
			}

			// check for changed falling conditions every 10 ticks (offset from activation checking by 5 ticks):
			// TODO split this over multiple ticks, for example by using a random per-entity offset
			boolean checkFallingConditions = ((tickCounter + 5) % 10 == 0);
			activeAIEntityCount = 0;
			activeGravityEntityCount = 0;
			Iterator<Entry<LivingEntity, EntityData>> iterator = entities.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<LivingEntity, EntityData> entry = iterator.next();
				LivingEntity entity = entry.getKey();
				EntityData entityData = entry.getValue();
				// entity still alive and loaded?
				if (entity.isDead() || !entity.isValid() || !ChunkCoords.isChunkLoaded(entity.getLocation(tempLocation))) {
					iterator.remove();
					this.onEntityRemoved(entity, entityData);
					continue;
				}
				ChunkData chunkData = entityData.chunkData;

				// handle gravity:
				gravityTimings.resume();
				if (chunkData.activeGravity) {
					activeGravityEntityCount++;

					// checking periodically if the entity is meant to fall, or if already falling:
					if (checkFallingConditions || entityData.falling) {
						Location entityLocation = entity.getLocation(tempLocation);
						entityData.distanceToGround = Utils.getCollisionDistanceToGround(entityLocation, GRAVITY_COLLISION_CHECK_RANGE);
						// not falling if the distance-to-ground is below the threshold:
						entityData.falling = (entityData.distanceToGround >= DISTANCE_TO_GROUND_THRESHOLD);
					}

					// handle falling:
					if (entityData.falling) {
						this.handleFalling(entity, entityData);
					}
				}
				gravityTimings.pause();

				// handle AI:
				aiTimings.resume();
				if (chunkData.activeAI) {
					activeAIEntityCount++;

					// only handle AI if not currently falling:
					if (!entityData.falling) {
						this.handleAI(entity);
					}
				}
				aiTimings.pause();
			}
			// cleanup temporarily used location object:
			tempLocation.setWorld(null);

			// stop the task if there are no entities with AI anymore:
			if (entities.isEmpty()) {
				this.stop();
			}

			// timings:
			totalTimings.stop();
			gravityTimings.stop();
			aiTimings.stop();

			currentlyRunning = false;
		}, 1L, 1L);
	}

	public void stop() {
		if (aiTask != null) {
			aiTask.cancel();
			aiTask = null;
			this.resetStatistics();
		}
		// cleanup here as well, just in case:
		// activeAIChunks.clear();
		// activeGravityChunks.clear();
	}

	public boolean isActive() {
		if (aiTask == null) return false;
		// checking this here, since something else might cancel our task from outside:
		return (currentlyRunning || Bukkit.getScheduler().isQueued(aiTask.getTaskId()));
	}

	public void addEntity(LivingEntity entity) {
		Validate.notNull(entity, "Entity is null!");
		Validate.isTrue(!entity.isDead() && entity.isValid(), "Entity is invalid!");
		Validate.isTrue(!currentlyRunning, "Cannot add entities while the ai task is running!");
		if (entities.containsKey(entity)) return;

		// determine entity chunk (asserts that the entity won't move!):
		Chunk entityChunk = entity.getLocation(tempLocation).getChunk();
		tempLocation.setWorld(null); // cleanup temporarily used location object
		
		// active gravity handling?
		boolean gravityActive = this.isGravityActive();

		// add chunk entry:
		ChunkData chunkData = activeChunks.get(entityChunk);
		if (chunkData == null) {
			chunkData = new ChunkData(entityChunk, gravityActive);
			activeChunks.put(entityChunk, chunkData);
		}
		chunkData.entityCount++;

		// add entity entry:
		entities.put(entity, new EntityData(chunkData, gravityActive));// new EntityData(entityChunk, tickCounter + 20));

		// start the ai task, if it isn't already running:
		this.start();
	}

	public void removeEntity(LivingEntity entity) {
		Validate.isTrue(!currentlyRunning, "Cannot remove entities while the ai task is running!");
		// remove entity:
		EntityData entityData = entities.remove(entity);
		if (entityData != null) {
			this.onEntityRemoved(entity, entityData);
		}
	}

	private void onEntityRemoved(LivingEntity entity, EntityData entityData) {
		assert entity != null && entityData != null;
		// update/remove chunk entry:
		ChunkData chunkData = entityData.chunkData;
		chunkData.entityCount--;
		if (chunkData.entityCount <= 0) {
			activeChunks.remove(chunkData.chunk);
		}
	}

	public void reset() {
		Validate.isTrue(!currentlyRunning, "Cannot reset while the ai task is running!");
		entities.clear();
		// activeChunks.clear();
		this.resetStatistics();
	}

	public void resetStatistics() {
		// reset statistics:
		activeAIChunksCount = 0;
		activeAIEntityCount = 0;

		activeGravityChunksCount = 0;
		activeGravityEntityCount = 0;

		totalTimings.reset();
		activationTimings.reset();
		gravityTimings.reset();
		aiTimings.reset();
	}

	// statistics:

	public int getEntityCount() {
		return entities.size();
	}

	public int getActiveAIChunksCount() {
		return activeAIChunksCount;
	}

	public int getActiveAIEntityCount() {
		return activeAIEntityCount;
	}

	public int getActiveGravityChunksCount() {
		return activeGravityChunksCount;
	}

	public int getActiveGravityEntityCount() {
		return activeGravityEntityCount;
	}

	public Timings getTotalTimings() {
		return totalTimings;
	}

	public Timings getActivationTimings() {
		return activationTimings;
	}

	public Timings getGravityTimings() {
		return gravityTimings;
	}

	public Timings getAITimings() {
		return aiTimings;
	}

	// handling:

	private static enum ActivationType {
		GRAVITY,
		AI;
	}

	private void activateNearbyChunks(Chunk centerChunk, int chunkRadius, ActivationType activationType) {
		assert centerChunk != null && chunkRadius >= 0 && activationType != null;
		World world = centerChunk.getWorld();
		int minX = centerChunk.getX() - chunkRadius;
		int minZ = centerChunk.getZ() - chunkRadius;
		int maxX = centerChunk.getX() + chunkRadius;
		int maxZ = centerChunk.getZ() + chunkRadius;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!world.isChunkLoaded(x, z)) continue;
				Chunk chunk = world.getChunkAt(x, z);
				ChunkData chunkData = activeChunks.get(chunk);
				if (chunkData == null) continue;

				switch (activationType) {
				case GRAVITY:
					if (!chunkData.activeGravity) {
						chunkData.activeGravity = true;
						activeGravityChunksCount++;
					}
					break;
				case AI:
					if (!chunkData.activeAI) {
						chunkData.activeAI = true;
						activeAIChunksCount++;
					}
				default:
					// not expected
					break;
				}
			}
		}
	}

	// gets run every tick while falling:
	private void handleFalling(LivingEntity entity, EntityData entityData) {
		assert entityData.falling && entityData.distanceToGround >= DISTANCE_TO_GROUND_THRESHOLD;
		// determine falling step size:
		double fallingStepSize;
		double remainingDistance = (entityData.distanceToGround - MAX_FALLING_DISTANCE_PER_TICK);
		if (remainingDistance <= DISTANCE_TO_GROUND_THRESHOLD) {
			// we are nearly there: let's position the entity exactly on the ground and stop the falling
			fallingStepSize = entityData.distanceToGround;
			entityData.falling = false;
		} else {
			fallingStepSize = MAX_FALLING_DISTANCE_PER_TICK;
			// we continue the falling and check for collisions again in the next tick
		}

		// teleport the entity to its new location:
		Location newLocation = entity.getLocation(tempLocation);
		newLocation.add(0.0D, -fallingStepSize, 0.0D);
		entity.teleport(newLocation);
		tempLocation.setWorld(null); // cleanup temporarily used location object
	}

	// gets run every tick while in range of players:
	private void handleAI(LivingEntity entity) {
		// look at nearby players: implemented by manually running the vanilla ai goal
		NMSManager.getProvider().tickAI(entity);
	}
}
