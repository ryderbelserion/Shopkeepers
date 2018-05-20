package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

/**
 * Reports whether the server uses player shopkeepers.
 */
public class PlayerShopsChart extends Metrics.SimplePie {

	public PlayerShopsChart(ShopkeepersPlugin plugin) {
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
