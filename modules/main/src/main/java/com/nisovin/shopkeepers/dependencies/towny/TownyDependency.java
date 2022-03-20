package com.nisovin.shopkeepers.dependencies.towny;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;

public final class TownyDependency {

	public static final String PLUGIN_NAME = "Towny";

	public static @Nullable Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
	}

	public static boolean isCommercialArea(Location location) {
		if (!isPluginEnabled()) return false;
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
		return (townBlock != null && townBlock.getType() == TownBlockType.COMMERCIAL);
	}

	private TownyDependency() {
	}
}
