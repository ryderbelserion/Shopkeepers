package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;

/**
 * Checks whether this server is using WorldGuard.
 */
public class WorldGuardChart extends Metrics.SimplePie {

	public WorldGuardChart() {
		super("uses_worldguard", () -> (WorldGuardHandler.isPluginEnabled()) ? "Yes" : "No");
	}
}
