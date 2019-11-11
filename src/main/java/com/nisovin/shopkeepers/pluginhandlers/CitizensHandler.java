package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class CitizensHandler {

	public static final String PLUGIN_NAME = "Citizens";

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
	}

	public static boolean isNPC(Entity entity) {
		return entity.hasMetadata("NPC");
	}

	private CitizensHandler() {
	}
}
