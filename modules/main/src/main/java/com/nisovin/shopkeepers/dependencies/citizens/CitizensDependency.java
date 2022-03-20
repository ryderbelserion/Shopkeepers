package com.nisovin.shopkeepers.dependencies.citizens;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CitizensDependency {

	public static final String PLUGIN_NAME = "Citizens";

	public static @Nullable Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
	}

	private CitizensDependency() {
	}
}
