package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopkeeper;

/**
 * Reports whether the server uses player shopkeepers.
 */
public class PlayerShopsChart extends Metrics.SimplePie {

	public PlayerShopsChart(SKShopkeepersPlugin plugin) {
		super("uses_player_shops", () -> {
			for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					return "Yes";
				}
			}
			return "No";
		});
	}
}
