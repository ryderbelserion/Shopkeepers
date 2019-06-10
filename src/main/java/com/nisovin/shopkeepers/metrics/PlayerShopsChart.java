package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;

/**
 * Reports whether the server uses player shopkeepers.
 */
public class PlayerShopsChart extends Metrics.SimplePie {

	public PlayerShopsChart(SKShopkeeperRegistry shopkeeperRegistry) {
		super("uses_player_shops", () -> {
			return (shopkeeperRegistry.getPlayerShopCount() > 0) ? "Yes" : "No";
		});
	}
}
