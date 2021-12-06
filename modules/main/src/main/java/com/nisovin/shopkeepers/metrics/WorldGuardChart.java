package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency;

/**
 * Checks whether this server is using WorldGuard.
 */
public class WorldGuardChart extends Metrics.SimplePie {

	public WorldGuardChart() {
		super("uses_worldguard", () -> (WorldGuardDependency.isPluginEnabled()) ? "Yes" : "No");
	}
}
