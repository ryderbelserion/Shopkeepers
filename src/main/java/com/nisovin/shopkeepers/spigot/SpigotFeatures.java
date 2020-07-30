package com.nisovin.shopkeepers.spigot;

public class SpigotFeatures {

	// Null if not yet checked:
	private static Boolean SPIGOT_AVAILABLE = null;

	public static boolean isSpigotAvailable() {
		if (SPIGOT_AVAILABLE == null) {
			// Not yet checked:
			try {
				Class.forName("org.bukkit.Server$Spigot");
				SPIGOT_AVAILABLE = true;
			} catch (ClassNotFoundException e) {
				SPIGOT_AVAILABLE = false;
			}
		}
		return SPIGOT_AVAILABLE;
	}

	private SpigotFeatures() {
	}
}
