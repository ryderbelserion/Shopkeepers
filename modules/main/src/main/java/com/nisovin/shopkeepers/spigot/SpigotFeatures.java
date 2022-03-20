package com.nisovin.shopkeepers.spigot;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class SpigotFeatures {

	// Null if not yet checked:
	private static @Nullable Boolean SPIGOT_AVAILABLE = null;

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
		assert SPIGOT_AVAILABLE != null;
		return SPIGOT_AVAILABLE;
	}

	private SpigotFeatures() {
	}
}
