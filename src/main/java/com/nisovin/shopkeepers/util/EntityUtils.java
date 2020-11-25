package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class EntityUtils {

	private EntityUtils() {
	}

	public static EntityType matchEntityType(String entityTypeId) {
		if (StringUtils.isEmpty(entityTypeId)) return null;
		// Get by Bukkit id:
		String normalizedEntityTypeId = entityTypeId.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		try {
			return EntityType.valueOf(normalizedEntityTypeId);
		} catch (IllegalArgumentException e) {
			// Unknown entity type:
			return null;
		}
	}

	public static void printEntityCounts(Chunk chunk) {
		Map<EntityType, Integer> entityCounts = new EnumMap<>(EntityType.class);
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
		Log.info("Entities of chunk " + TextUtils.getChunkString(chunk) + " (total: " + entities.length + "): " + entityCounts);
	}

	public static List<Entity> getNearbyEntities(Location location, double radius, EntityType... types) {
		List<Entity> entities = new ArrayList<>();
		if (location == null) return entities;
		if (radius <= 0.0D) return entities;

		List<EntityType> typesList = (types == null) ? Collections.<EntityType>emptyList() : Arrays.asList(types);
		double radius2 = radius * radius;
		int chunkRadius = ((int) (radius / 16)) + 1;
		Chunk center = location.getChunk();
		int startX = center.getX() - chunkRadius;
		int endX = center.getX() + chunkRadius;
		int startZ = center.getZ() - chunkRadius;
		int endZ = center.getZ() + chunkRadius;
		World world = location.getWorld();
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk chunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : chunk.getEntities()) {
					Location entityLoc = entity.getLocation();
					// TODO This is a workaround: For some yet unknown reason entities sometimes report to be in a
					// different world..
					if (!entityLoc.getWorld().equals(world)) {
						Log.debug(() -> "Found an entity which reports to be in a different world than the chunk we got it from: "
								+ "Location=" + location + ", Chunk=" + chunk + ", ChunkWorld=" + chunk.getWorld()
								+ ", entityType=" + entity.getType() + ", entityLocation=" + entityLoc);
						continue; // Skip this entity
					}

					if (entityLoc.distanceSquared(location) <= radius2) {
						if (typesList.isEmpty() || typesList.contains(entity.getType())) {
							entities.add(entity);
						}
					}
				}
			}
		}
		return entities;
	}

	public static List<Entity> getNearbyChunkEntities(Chunk chunk, int chunkRadius, boolean loadChunks, EntityType... types) {
		List<Entity> entities = new ArrayList<>();
		if (chunk == null) return entities;
		if (chunkRadius < 0) return entities;

		List<EntityType> typesList = (types == null) ? Collections.<EntityType>emptyList() : Arrays.asList(types);
		int startX = chunk.getX() - chunkRadius;
		int endX = chunk.getX() + chunkRadius;
		int startZ = chunk.getZ() - chunkRadius;
		int endZ = chunk.getZ() + chunkRadius;
		World world = chunk.getWorld();
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!loadChunks && !world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk currentChunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : currentChunk.getEntities()) {
					Location entityLoc = entity.getLocation();
					// TODO This is a workaround: For some yet unknown reason entities sometimes report to be in a
					// different world..
					if (!entityLoc.getWorld().equals(world)) {
						Log.debug(() -> "Found an entity which reports to be in a different world than the chunk we got it from: "
								+ "Chunk=" + currentChunk + ", ChunkWorld=" + currentChunk.getWorld()
								+ ", entityType=" + entity.getType() + ", entityLocation=" + entityLoc);
						continue; // Skip this entity
					}

					if (typesList.isEmpty() || typesList.contains(entity.getType())) {
						entities.add(entity);
					}
				}
			}
		}
		return entities;
	}

	private static final int ENTITY_TARGET_RANGE = 10;

	public static Entity getTargetedEntity(Player player) {
		Location playerLoc = player.getEyeLocation();
		World world = playerLoc.getWorld();
		Vector viewDirection = playerLoc.getDirection();

		// Ray trace to check for the closest block and entity collision:
		// We ignore passable blocks to make the targeting easier.
		RayTraceResult rayTraceResult = world.rayTrace(playerLoc, viewDirection, ENTITY_TARGET_RANGE, FluidCollisionMode.NEVER, true, 0.0D, (entity) -> {
			return !entity.isDead() && !entity.equals(player); // TODO SPIGOT-5228: Filtering dead entities.
		});
		if (rayTraceResult != null) {
			return rayTraceResult.getHitEntity(); // Can be null
		}
		return null;
	}
}
