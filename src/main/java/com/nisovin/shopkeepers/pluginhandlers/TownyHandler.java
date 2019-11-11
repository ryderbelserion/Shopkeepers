package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyHandler {

	public static final String PLUGIN_NAME = "Towny";

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
	}

	public static boolean isCommercialArea(Location location) {
		if (!isPluginEnabled()) return false;
		TownBlock townBlock = TownyUniverse.getTownBlock(location);
		return (townBlock.getType() == TownBlockType.COMMERCIAL);
	}

	private TownyHandler() {
	}
}
