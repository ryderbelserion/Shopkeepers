package com.nisovin.shopkeepers.metrics;

import java.util.Map;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;

/**
 * Reports the number of worlds containing shopkeepers.
 */
public class WorldsChart extends Metrics.SimplePie {

	public WorldsChart(SKShopkeeperRegistry shopkeeperRegistry) {
		super("worlds_with_shops", () -> {
			// only contains entries for worlds with at least one shopkeeper
			Map<String, Integer> shopsByWorld = shopkeeperRegistry.getShopkeeperCountsByWorld();
			int worldsWithShops = shopsByWorld.keySet().size();
			if (shopsByWorld.containsKey(null)) { // exclude virtual shops from this metric
				worldsWithShops -= 1;
			}
			return String.valueOf(worldsWithShops);
		});
	}
}
