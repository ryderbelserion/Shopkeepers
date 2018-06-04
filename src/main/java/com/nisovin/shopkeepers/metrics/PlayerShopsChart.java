package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.registry.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopkeeper;

/**
 * Reports whether the server uses player shopkeepers.
 */
public class PlayerShopsChart extends Metrics.SimplePie {

	public PlayerShopsChart(ShopkeeperRegistry shopkeeperRegistry) {
		super("uses_player_shops", () -> {
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					return "Yes";
				}
			}
			return "No";
		});
	}
}
