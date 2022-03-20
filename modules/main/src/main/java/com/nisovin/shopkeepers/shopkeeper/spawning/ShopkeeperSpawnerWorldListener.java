package com.nisovin.shopkeepers.shopkeeper.spawning;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Handles events related to shopkeeper spawning.
 */
class ShopkeeperSpawnerWorldListener implements Listener {

	private final ShopkeeperSpawner spawner;
	private final WorldSaveDespawner worldSaveDespawner;

	ShopkeeperSpawnerWorldListener(
			ShopkeeperSpawner spawner,
			WorldSaveDespawner worldSaveDespawner
	) {
		Validate.notNull(spawner, "spawner is null");
		Validate.notNull(worldSaveDespawner, "worldSaveDespawner is null");
		this.spawner = spawner;
		this.worldSaveDespawner = worldSaveDespawner;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldSave(WorldSaveEvent event) {
		World world = event.getWorld();
		worldSaveDespawner.onWorldSave(world);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldUnload(WorldUnloadEvent event) {
		World world = event.getWorld();
		spawner.onWorldUnload(world);
		worldSaveDespawner.onWorldUnload(world);
	}
}
