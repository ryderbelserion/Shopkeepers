package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class CreatureForceSpawnListener implements Listener {

	private @Nullable Location nextSpawnLocation = null;
	private @Nullable EntityType nextEntityType = null;

	CreatureForceSpawnListener() {
	}

	// This listener tries to bypass other plugins which block the spawning of living shopkeeper
	// entities.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onCreatureSpawn(CreatureSpawnEvent event) {
		if (nextSpawnLocation == null) return;
		if (this.matchesForcedCreatureSpawn(event)) {
			event.setCancelled(false);
		} else {
			// This should normally not be reached.
			Log.debug(() -> "Shopkeeper entity-spawning seems to be out of sync: "
					+ "spawn-force was activated for an entity of type " + nextEntityType
					+ " at location " + nextSpawnLocation + ", but a different entity of type "
					+ event.getEntityType() + " was spawned at location " + event.getLocation()
					+ ".");
		}
		nextSpawnLocation = null;
		nextEntityType = null;
	}

	private boolean matchesForcedCreatureSpawn(CreatureSpawnEvent event) {
		return event.getEntityType() == nextEntityType
				&& LocationUtils.getSafeDistanceSquared(event.getLocation(), nextSpawnLocation) < 0.6D;
	}

	void forceCreatureSpawn(@Nullable Location location, @Nullable EntityType entityType) {
		this.nextSpawnLocation = location;
		this.nextEntityType = entityType;
	}
}
