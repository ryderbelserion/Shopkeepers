package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.dependencies.citizens.CitizensDependency;

/**
 * Checks whether this server is using Citizens.
 */
public class CitizensChart extends Metrics.SimplePie {

	public CitizensChart() {
		super("uses_citizens", () -> (CitizensDependency.isPluginEnabled()) ? "Yes" : "No");
	}
}
