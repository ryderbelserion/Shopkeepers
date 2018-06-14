package com.nisovin.shopkeepers.metrics;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * Reports the number of worlds containing shopkeepers.
 */
public class WorldsChart extends Metrics.SimplePie {

	public WorldsChart(ShopkeeperRegistry shopkeeperRegistry) {
		super("worlds_with_shops", () -> {
			Set<String> worlds = new HashSet<>();
			for (Entry<ChunkCoords, ?> byChunkEntry : shopkeeperRegistry.getAllShopkeepersByChunks().entrySet()) {
				worlds.add(byChunkEntry.getKey().getWorldName());
			}
			return String.valueOf(worlds.size());
		});
	}
}
