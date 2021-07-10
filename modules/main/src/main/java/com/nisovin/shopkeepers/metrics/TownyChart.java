package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;

/**
 * Checks whether this server is using Towny.
 */
public class TownyChart extends Metrics.SimplePie {

	public TownyChart() {
		super("uses_towny", () -> (TownyHandler.isPluginEnabled()) ? "Yes" : "No");
	}
}
