package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;

/**
 * Reports the number of worlds containing shopkeepers.
 */
public class WorldsChart extends Metrics.SimplePie {

	public WorldsChart(ShopkeeperRegistry shopkeeperRegistry) {
		super("worlds_with_shops", () -> {
			return String.valueOf(shopkeeperRegistry.getWorldsWithShopkeepers().size());
		});
	}
}
