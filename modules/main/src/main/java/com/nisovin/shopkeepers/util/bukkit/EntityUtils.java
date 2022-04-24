package com.nisovin.shopkeepers.util.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.PredicateUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class EntityUtils {

	private static final Set<@NonNull Material> LAVA = Collections.singleton(Material.LAVA);

	/**
	 * Gets the types of fluids this type of entity can stand on.
	 * <p>
	 * For example, striders and magma cubes can stand on lava.
	 * 
	 * @param entityType
	 *            the entity type
	 * @return the fluids on which entities of this type can stand, not <code>null</code> but can be
	 *         empty
	 */
	public static Set<? extends @NonNull Material> getCollidableFluids(EntityType entityType) {
		// Even though blazes sink to the ground in vanilla Minecraft, we allow them to stand on top
		// of lava because that makes them more useful in nether themed shops. They are also able to
		// fly and hover in mid-air, so this deviation from vanilla is not that severe.
		switch (entityType) {
		case STRIDER:
		case MAGMA_CUBE:
		case BLAZE:
			return LAVA;
		default:
			return Collections.emptySet();
		}
	}

	/**
	 * Checks if entities of the given type may in some circumstances despawn when the world's
	 * difficulty is set to {@link Difficulty#PEACEFUL}.
	 * <p>
	 * This does not take any entity instance specific conditions into account. For example, when
	 * the difficulty is set to peaceful, slimes only despawn if their size is greater than
	 * <code>0</code> (<code>1</code> in Bukkit). This method does not take these aspects into
	 * account and instead returns <code>true</code> for slimes.
	 * 
	 * @param entityType
	 *            the entity type, not <code>null</code>
	 * @return <code>true</code> if entities of the given type are in some circumstances despawned
	 *         due to the world's difficulty being set to peaceful
	 */
	public static boolean isRemovedOnPeacefulDifficulty(EntityType entityType) {
		assert entityType != null;
		// TODO Replace with enum constants once we only support MC 1.16+
		switch (entityType.name()) {
		case "PIGLIN":
			return false;
		case "SLIME":
		case "GHAST":
		case "PHANTOM":
			return true;
		default:
			Class<?> entityClass = entityType.getEntityClass();
			if (entityClass != null) {
				return Monster.class.isAssignableFrom(entityClass);
			} else {
				return false;
			}
		}
	}

	/**
	 * Checks if the given entity is a {@link ComplexEntityPart} and then returns its
	 * {@link ComplexEntityPart#getParent() parent}. Otherwise, returns the given entity itself.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return the resolved entity, not <code>null</code>
	 */
	public static Entity resolveComplexEntity(Entity entity) {
		if (entity instanceof ComplexEntityPart) {
			return ((ComplexEntityPart) entity).getParent();
		} else {
			return entity;
		}
	}

	/**
	 * Parses the {@link EntityType} from the given name.
	 * 
	 * @param entityTypeName
	 *            the entity type name, can be <code>null</code>
	 * @return the parsed {@link EntityType}, or <code>null</code> if the entity type cannot be
	 *         parsed
	 */
	public static @Nullable EntityType parseEntityType(@Nullable String entityTypeName) {
		return MinecraftEnumUtils.parseEnum(EntityType.class, entityTypeName);
	}

	public static void printEntityCounts(Chunk chunk) {
		Map<@NonNull EntityType, Integer> entityCounts = new EnumMap<>(EntityType.class);
		Entity[] entities = chunk.getEntities();
		for (Entity entity : entities) {
			EntityType entityType = entity.getType();
			Integer entityCount = entityCounts.get(entityType);
			if (entityCount == null) {
				entityCount = 0;
			}
			entityCount += 1;
			entityCounts.put(entityType, entityCount);
		}
		Log.info("Entities of chunk " + TextUtils.getChunkString(chunk)
				+ " (total: " + entities.length + "): " + entityCounts);
	}

	// If acceptedTypes is empty, the returned Predicate accepts all entity types.
	public static Predicate<Entity> filterByType(Set<? extends @NonNull EntityType> acceptedTypes) {
		if (acceptedTypes.isEmpty()) {
			return PredicateUtils.alwaysTrue();
		} else {
			return (entity) -> acceptedTypes.contains(entity.getType());
		}
	}

	// If searchedTypes is empty, this includes all entity types.
	public static List<@NonNull Entity> getNearbyEntities(
			Location location,
			double radius,
			boolean loadChunks,
			Set<? extends @NonNull EntityType> searchedTypes
	) {
		return getNearbyEntities(location, radius, loadChunks, filterByType(searchedTypes));
	}

	public static List<@NonNull Entity> getNearbyEntities(
			Location location,
			double radius,
			boolean loadChunks,
			Predicate<? super @NonNull Entity> filter
	) {
		Validate.notNull(filter, "filter is null");

		// Throws an exception if the world is not available:
		World world = LocationUtils.getWorld(location);
		int centerChunkX = location.getBlockX() >> 4;
		int centerChunkZ = location.getBlockZ() >> 4;
		int chunkRadius = ((int) (radius / 16)) + 1;
		double radius2 = radius * radius;
		Predicate<@NonNull Entity> combinedFilter = (entity) -> {
			Location entityLoc = entity.getLocation();
			if (entityLoc.distanceSquared(location) > radius2) {
				return false;
			}
			return filter.test(entity);
		};
		return getNearbyChunkEntities(
				world,
				centerChunkX,
				centerChunkZ,
				chunkRadius,
				loadChunks,
				combinedFilter
		);
	}

	// If searchedTypes is empty, this includes all entity types.
	// chunkRadius of 0: Search only within the given chunk.
	public static List<@NonNull Entity> getNearbyChunkEntities(
			Chunk chunk,
			int chunkRadius,
			boolean loadChunks,
			Set<? extends @NonNull EntityType> searchedTypes
	) {
		return getNearbyChunkEntities(chunk, chunkRadius, loadChunks, filterByType(searchedTypes));
	}

	// chunkRadius of 0: Search only within the given chunk.
	// filter of null: Accepts all found entities.
	public static List<@NonNull Entity> getNearbyChunkEntities(
			Chunk chunk,
			int chunkRadius,
			boolean loadChunks,
			Predicate<? super @NonNull Entity> filter
	) {
		Validate.notNull(chunk, "chunk is null");
		return getNearbyChunkEntities(
				chunk.getWorld(),
				chunk.getX(),
				chunk.getZ(),
				chunkRadius,
				loadChunks,
				filter
		);
	}

	// chunkRadius of 0: Search only within the center chunk.
	// filter of null: Accepts all found entities.
	public static List<@NonNull Entity> getNearbyChunkEntities(
			World world,
			int centerChunkX,
			int centerChunkZ,
			int chunkRadius,
			boolean loadChunks,
			Predicate<? super @NonNull Entity> filter
	) {
		Validate.notNull(world, "world is null");
		Validate.notNull(filter, "filter is null");

		// Assert: World is loaded.
		List<@NonNull Entity> entities = new ArrayList<>();
		if (chunkRadius < 0) return entities;

		int startX = centerChunkX - chunkRadius;
		int endX = centerChunkX + chunkRadius;
		int startZ = centerChunkZ - chunkRadius;
		int endZ = centerChunkZ + chunkRadius;
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!loadChunks && !world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk currentChunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : currentChunk.getEntities()) {
					// TODO This is a workaround: For some yet unknown reason entities sometimes
					// report to be in a different world..
					if (!entity.getWorld().equals(world)) {
						Log.debug(() -> "Found an entity which reports to be in a different world "
								+ "than the chunk we got it from: "
								+ "Chunk=" + currentChunk
								+ ", ChunkWorld=" + currentChunk.getWorld()
								+ ", entityType=" + entity.getType()
								+ ", entityLocation=" + entity.getLocation());
						continue; // Skip this entity
					}

					if (filter.test(entity)) {
						entities.add(entity);
					}
				}
			}
		}
		return entities;
	}

	private static final int ENTITY_TARGET_RANGE = 10;

	public static @Nullable Entity getTargetedEntity(Player player) {
		return getTargetedEntity(player, PredicateUtils.alwaysTrue());
	}

	public static @Nullable Entity getTargetedEntity(
			Player player,
			Predicate<? super @NonNull Entity> filter
	) {
		Validate.notNull(filter, "filter is null");
		Location playerLoc = player.getEyeLocation();
		World world = Unsafe.assertNonNull(playerLoc.getWorld());
		Vector viewDirection = playerLoc.getDirection();

		// Ray trace to check for the closest block and entity collision:
		// We ignore passable blocks to make the targeting easier.
		RayTraceResult rayTraceResult = world.rayTrace(
				playerLoc,
				viewDirection,
				ENTITY_TARGET_RANGE,
				FluidCollisionMode.NEVER,
				true,
				0.0D,
				(entity) -> {
					if (entity.isDead()) return false; // TODO SPIGOT-5228: Ignore dead entities.
					if (entity.equals(player)) return false; // Ignore player
					return filter.test(entity); // Check custom filter
				}
		);
		if (rayTraceResult != null) {
			return rayTraceResult.getHitEntity(); // Can be null
		}
		return null;
	}

	private EntityUtils() {
	}
}
