package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;

/**
 * Reports whether the server uses player shopkeepers.
 */
public class PlayerShopsChart extends Metrics.SimplePie {

	public PlayerShopsChart(ShopkeeperRegistry shopkeeperRegistry) {
		super("uses_player_shops", () -> {
			return (shopkeeperRegistry.getAllPlayerShopkeepers().size() > 0) ? "Yes" : "No";
		});
	}
}
