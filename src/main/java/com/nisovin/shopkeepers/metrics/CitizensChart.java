package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;

/**
 * Checks whether this server is using Citizens.
 */
public class CitizensChart extends Metrics.SimplePie {

	public CitizensChart() {
		super("uses_citizens", () -> (CitizensHandler.isPluginEnabled()) ? "Yes" : "No");
	}
}
