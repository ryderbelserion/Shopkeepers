package com.nisovin.shopkeepers.util;

import org.bukkit.Bukkit;

public final class ServerUtils {

	private ServerUtils() {
	}

	public static String getCraftBukkitVersion() {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String cbVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		return cbVersion;
	}
}
