package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import com.nisovin.shopkeepers.dependencies.citizens.CitizensDependency;

class PluginListener implements Listener {

	private final CitizensShops citizensShops;

	PluginListener(CitizensShops citizensShops) {
		this.citizensShops = citizensShops;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPluginEnable(PluginEnableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(CitizensDependency.PLUGIN_NAME)) {
			citizensShops.enable();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPluginDisable(PluginDisableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(CitizensDependency.PLUGIN_NAME)) {
			citizensShops.disable();
		}
	}
}
