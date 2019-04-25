package com.nisovin.shopkeepers.villagers;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class BlockVillagerSpawnListener implements Listener {

	public BlockVillagerSpawnListener() {
	}

	@EventHandler(ignoreCancelled = true)
	void onSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == SpawnReason.CUSTOM) return; // ignore plugin spawns
		EntityType entityType = event.getEntityType();
		// prevent spawning of villagers, wandering traders and their trader llamas:
		if (entityType == EntityType.VILLAGER || entityType == EntityType.WANDERING_TRADER || entityType == EntityType.TRADER_LLAMA) {
			event.setCancelled(true);
		}
	}
}
