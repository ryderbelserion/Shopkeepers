package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;

/**
 * Reports the (rough) number of shopkeepers.
 */
public class ShopkeepersCountChart extends Metrics.SimplePie {

	public ShopkeepersCountChart(ShopkeeperRegistry shopkeeperRegistry) {
		super("shopkeepers_count", () -> {
			int numberOfShopkeepers = shopkeeperRegistry.getAllShopkeepers().size();
			if (numberOfShopkeepers >= 1000) return (numberOfShopkeepers / 1000) + "000+";
			else if (numberOfShopkeepers >= 500) return "500+";
			else if (numberOfShopkeepers >= 100) return "100+";
			else if (numberOfShopkeepers >= 10) return "10+";
			else if (numberOfShopkeepers > 0) return "<10";
			else return "0";
		});
	}
}
