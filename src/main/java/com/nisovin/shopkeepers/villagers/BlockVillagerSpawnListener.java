package com.nisovin.shopkeepers.villagers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;

import com.nisovin.shopkeepers.Settings;

// Prevents spawning of regular villagers (including wandering traders)
public class BlockVillagerSpawnListener implements Listener {

	public BlockVillagerSpawnListener() {
	}

	private boolean isSpawnBlockingBypassed(SpawnReason spawnReason) {
		switch (spawnReason) {
		case CUSTOM: // plugins
		case SPAWNER_EGG:
			// not obtainable in vanilla minecraft, regular item usage of shopkeeper creation item is handled separately
		case SPAWNER: // not obtainable in vanilla minecraft
		case CURED: // handled separately
			return true;
		default:
			return false;
		}
	}

	private boolean isSpawningBlocked(EntityType entityType) {
		if (Settings.blockVillagerSpawns && entityType == EntityType.VILLAGER) return true;
		if (Settings.blockWanderingTraderSpawns && (entityType == EntityType.WANDERING_TRADER || entityType == EntityType.TRADER_LLAMA)) return true;
		return false;
	}

	@EventHandler(ignoreCancelled = true)
	void onSpawn(CreatureSpawnEvent event) {
		SpawnReason spawnReason = event.getSpawnReason();
		if (this.isSpawnBlockingBypassed(spawnReason)) return;

		EntityType entityType = event.getEntityType();
		// prevent spawning of villagers, wandering traders and their trader llamas:
		if (this.isSpawningBlocked(entityType)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onChunkLoad(ChunkLoadEvent event) {
		// remove villagers that got spawned as part of chunk generation:
		if (!event.isNewChunk()) return;
		for (Entity entity : event.getChunk().getEntities()) {
			EntityType entityType = entity.getType();
			if (this.isSpawningBlocked(entityType)) {
				entity.remove();
			}
		}
	}
}
