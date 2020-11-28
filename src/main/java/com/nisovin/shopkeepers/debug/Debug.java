package com.nisovin.shopkeepers.debug;

import org.bukkit.Bukkit;

import com.nisovin.shopkeepers.Settings;

public class Debug {

	public static boolean isDebugging() {
		return isDebugging(null);
	}

	public static boolean isDebugging(String option) {
		if (Bukkit.isPrimaryThread()) {
			return Settings.debug && (option == null || Settings.debugOptions.contains(option));
		} else {
			Settings.AsyncSettings async = Settings.async();
			return async.debug && (option == null || async.debugOptions.contains(option));
		}
	}

	private Debug() {
	}
}
