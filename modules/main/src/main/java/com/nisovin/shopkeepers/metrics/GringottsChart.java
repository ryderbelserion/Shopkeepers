package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;

/**
 * Checks whether this server is using Gringotts.
 */
public class GringottsChart extends Metrics.SimplePie {

	private static final String GRINGOTTS_PLUGIN_NAME = "Gringotts";

	public GringottsChart() {
		super("uses_gringotts", () -> {
			return Bukkit.getPluginManager().isPluginEnabled(GRINGOTTS_PLUGIN_NAME) ? "Yes" : "No";
		});
	}
}
