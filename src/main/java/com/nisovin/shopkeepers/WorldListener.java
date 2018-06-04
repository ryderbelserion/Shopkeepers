package com.nisovin.shopkeepers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.Log;

class WorldListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;

	WorldListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (chunk.isLoaded()) {
				shopkeeperRegistry.loadShopkeepersInChunk(chunk);
			}
		}, 2);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkUnload(ChunkUnloadEvent event) {
		// living entity shopkeeper entities can wander into different chunks, so we remove all living entity shopkeeper
		// entities which wandered ways from their chunk during chunk unloads:
		Chunk chunk = event.getChunk();
		for (Entity entity : chunk.getEntities()) {
			Shopkeeper shopkeeper = shopkeeperRegistry.getLivingEntityShopkeeper(entity);
			if (shopkeeper != null && !shopkeeper.getChunkCoords().isSameChunk(chunk)) {
				Log.debug("Removing shop entity which was pushed away from shop's chunk at (" + shopkeeper.getPositionString() + ")");
				entity.remove();
			}
		}

		shopkeeperRegistry.unloadShopkeepersInChunk(chunk);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onWorldSave(WorldSaveEvent event) {
		World world = event.getWorld();
		UUID worldUID = world.getUID();
		Log.debug("World '" + world.getName() + "' is about to get saved: Unloading all shopkeepers in that world.");
		shopkeeperRegistry.unloadShopkeepersInWorld(world);
		Bukkit.getScheduler().runTask(plugin, () -> {
			// check if the world is still loaded:
			World loadedWorld = Bukkit.getWorld(worldUID);
			if (loadedWorld != null) {
				Log.debug("World '" + loadedWorld.getName() + "' was saved. Reloading all shopkeepers in that world.");
				shopkeeperRegistry.loadShopkeepersInWorld(loadedWorld);
			}
		});
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onWorldLoad(WorldLoadEvent event) {
		shopkeeperRegistry.loadShopkeepersInWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onWorldUnload(WorldUnloadEvent event) {
		shopkeeperRegistry.unloadShopkeepersInWorld(event.getWorld());
	}
}
