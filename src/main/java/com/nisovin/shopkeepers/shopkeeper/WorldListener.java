package com.nisovin.shopkeepers.shopkeeper;

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

class WorldListener implements Listener {

	// TODO: Unload shopkeepers on HIGHEST priority instead, so that monitoring plugins can determine the actually
	// unloaded (saved) entities / blocks? However, it is important to not unload them if the event gets cancelled.
	// Possible workaround: Unload on HIGHEST priority and then check and respawn on MONITOR priority in case the event
	// got cancelled? For now, keep it at MONITOR until an actual usecase comes up.

	private final SKShopkeeperRegistry shopkeeperRegistry;

	WorldListener(SKShopkeeperRegistry shopkeeperRegistry) {
		this.shopkeeperRegistry = shopkeeperRegistry;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		shopkeeperRegistry.onChunkLoad(chunk);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkUnload(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		shopkeeperRegistry.onChunkUnload(chunk);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldLoad(WorldLoadEvent event) {
		World world = event.getWorld();
		shopkeeperRegistry.onWorldLoad(world);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldUnload(WorldUnloadEvent event) {
		World world = event.getWorld();
		shopkeeperRegistry.onWorldUnload(world);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onWorldSave(WorldSaveEvent event) {
		World world = event.getWorld();
		shopkeeperRegistry.onWorldSave(world);
	}
}
