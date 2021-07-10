package com.nisovin.shopkeepers.shopkeeper;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Processes events that have to do with shopkeeper activation, spawning, despawning, etc.
 */
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

	// We react to player joins and teleports in order to quickly activate chunks around players that suddenly appear
	// near shopkeepers:

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		// Check if the player is still online (some other plugin might have kicked the player during the event):
		Player player = event.getPlayer();
		if (!player.isOnline()) return; // Player is no longer online

		// Activate the chunks around the player after the server has completely handled the join:
		shopkeeperRegistry.activatePendingNearbyChunksDelayed(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerTeleport(PlayerTeleportEvent event) {
		// The target location can be null in some circumstances (eg. when a player enters an end gateway, but there is
		// no end world). We ignore the event in this case.
		Location targetLocation = event.getTo();
		if (targetLocation == null) return;

		// Activate the chunks around the player after the teleport:
		Player player = event.getPlayer();
		shopkeeperRegistry.activatePendingNearbyChunksDelayed(player);
	}
}
