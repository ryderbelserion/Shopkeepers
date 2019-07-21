package com.nisovin.shopkeepers.shopkeeper;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.util.Log;

class WorldListener implements Listener {

	// TODO: Unload shopkeepers on HIGHEST priority instead, so that monitoring plugins can determine the actually
	// unloaded (saved) entities / blocks? However, it is important to not unload them if the event gets cancelled.
	// Possible workaround: Unload on HIGHEST priority and then check and respawn on MONITOR priority in case the event
	// got cancelled? For now, keep it at MONITOR until an actual usecase comes up.

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;

	WorldListener(SKShopkeepersPlugin plugin, SKShopkeeperRegistry shopkeeperRegistry) {
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		// not loading shopkeepers for temporarily loaded chunks:
		// TODO Further improve, by stopping this task in case the chunk gets unloaded (and then reloaded) in the
		// meantime. So to only load the shopkeepers if the chunk has been loaded for x ticks in a row without unloads
		// in between.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (chunk.isLoaded()) {
				shopkeeperRegistry.loadShopkeepersInChunk(chunk);
			}
		}, 2);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkUnload(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		shopkeeperRegistry.unloadShopkeepersInChunk(chunk);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldSave(WorldSaveEvent event) {
		World world = event.getWorld();
		UUID worldUID = world.getUID();
		Log.debug("World '" + world.getName() + "' is about to get saved: Unloading all shopkeepers in that world.");
		shopkeeperRegistry.unloadShopkeepersInWorld(world, true);
		Bukkit.getScheduler().runTask(plugin, () -> {
			// check if the world is still loaded:
			World loadedWorld = Bukkit.getWorld(worldUID);
			if (loadedWorld != null) {
				Log.debug("World '" + loadedWorld.getName() + "' was saved. Reloading all shopkeepers in that world.");
				shopkeeperRegistry.loadShopkeepersInWorld(loadedWorld, true);
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldLoad(WorldLoadEvent event) {
		shopkeeperRegistry.loadShopkeepersInWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldUnload(WorldUnloadEvent event) {
		shopkeeperRegistry.unloadShopkeepersInWorld(event.getWorld());
	}
}
