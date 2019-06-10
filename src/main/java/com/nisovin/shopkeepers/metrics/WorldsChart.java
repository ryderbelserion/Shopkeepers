package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;

/**
 * Reports the number of worlds containing shopkeepers.
 */
public class WorldsChart extends Metrics.SimplePie {

	public WorldsChart(SKShopkeeperRegistry shopkeeperRegistry) {
		super("worlds_with_shops", () -> {
			// only contains entries for worlds with at least one shopkeeper
			int worldsWithShops = shopkeeperRegistry.getShopkeeperCountsByWorld().keySet().size();
			return String.valueOf(worldsWithShops);
		});
	}
}
