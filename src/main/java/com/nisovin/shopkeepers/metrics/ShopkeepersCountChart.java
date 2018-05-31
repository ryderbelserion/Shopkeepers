package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;

/**
 * Reports the (rough) number of shopkeepers.
 */
public class ShopkeepersCountChart extends Metrics.SimplePie {

	public ShopkeepersCountChart(SKShopkeepersPlugin plugin) {
		super("shopkeepers_count", () -> {
			int numberOfShopkeepers = plugin.getAllShopkeepers().size();
			if (numberOfShopkeepers >= 1000) return (numberOfShopkeepers / 1000) + "000+";
			else if (numberOfShopkeepers >= 500) return "500+";
			else if (numberOfShopkeepers >= 100) return "100+";
			else if (numberOfShopkeepers >= 10) return "10+";
			else return "<10";
		});
	}
}
