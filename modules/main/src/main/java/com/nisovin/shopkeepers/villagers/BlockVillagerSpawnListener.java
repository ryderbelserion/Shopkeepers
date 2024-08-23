package com.nisovin.shopkeepers.villagers;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Prevents spawning of regular villagers (including wandering traders).
 */
public class BlockVillagerSpawnListener implements Listener {

	public BlockVillagerSpawnListener() {
	}

	private boolean isSpawnBlockingBypassed(SpawnReason spawnReason) {
		switch (spawnReason) {
		case CUSTOM: // Plugins
		case SPAWNER_EGG:
			// Not obtainable in vanilla Minecraft. Normal usage of the shopkeeper creation item is
			// handled separately.
		case SPAWNER: // Not obtainable in vanilla Minecraft
		case CURED: // Handled separately
			return true;
		default:
			return false;
		}
	}

	private boolean isSpawningBlocked(EntityType entityType) {
		if (entityType == EntityType.VILLAGER) {
			return Settings.blockVillagerSpawns;
		} else if (entityType == EntityType.WANDERING_TRADER || entityType == EntityType.TRADER_LLAMA) {
			return Settings.blockWanderingTraderSpawns;
		} else {
			return false;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onSpawn(CreatureSpawnEvent event) {
		SpawnReason spawnReason = event.getSpawnReason();
		if (this.isSpawnBlockingBypassed(spawnReason)) return;

		EntityType entityType = event.getEntityType();
		// Prevent spawning of villagers, wandering traders and their trader llamas:
		if (this.isSpawningBlocked(entityType)) {
			Log.debug(() -> "Preventing mob spawn of " + entityType + " at "
					+ TextUtils.getLocationString(event.getLocation()));
			event.setCancelled(true);
		}
	}

	// LOW priority so that other plugins don't have to process those meant-to-be-removed entities.
	// Note: Entity loading is deferred from chunk loading since MC 1.17. See SPIGOT-6547.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onChunkLoad(ChunkLoadEvent event) {
		// Remove villagers that got spawned as part of chunk generation:
		if (!event.isNewChunk()) return;

		var chunk = event.getChunk();

		// If entities are not yet loaded: Handled during subsequent EntitiesLoadEvent.
		if (!chunk.isEntitiesLoaded()) return;

		// Remove villagers that got spawned as part of chunk generation or loading:
		this.removeSpawnBlockedEntities(Arrays.asList(chunk.getEntities()));
	}

	// LOW priority so that other plugins don't have to process those meant-to-be-removed entities.
	// Note: Entity loading is deferred from chunk loading since MC 1.17. See SPIGOT-6547.
	// Note: As per comment in the linked ticket, the chunk might already have been unloaded again.
	// In this case, the entities report as invalid and a subsequent EntitiesUnloadEvent will be
	// called for them some time later. If the chunk is currently loaded, the chunk's entity list
	// should match the entity list of the event (untested).
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onChunkEntitiesLoaded(EntitiesLoadEvent event) {
		var chunk = event.getChunk();
		if (!chunk.isLoaded()) return;

		// Remove villagers that got spawned as part of chunk generation or loading:
		this.removeSpawnBlockedEntities(event.getEntities());
	}

	private void removeSpawnBlockedEntities(List<Entity> entities) {
		entities.forEach(entity -> {
			EntityType entityType = entity.getType();
			if (this.isSpawningBlocked(entityType)) {
				Log.debug(() -> "Preventing mob spawn (chunk loading) of " + entityType + " at "
						+ TextUtils.getLocationString(entity.getLocation()));
				entity.remove();
			}
		});
	}
}
