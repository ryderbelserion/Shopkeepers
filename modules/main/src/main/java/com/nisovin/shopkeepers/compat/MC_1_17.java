package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.17 upwards.
public final class MC_1_17 {

	public static final Material GLOW_INK_SAC = CompatUtils.getMaterial("GLOW_INK_SAC");

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.17 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.17 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		return (GLOW_INK_SAC != null);
	}

	private MC_1_17() {
	}
}
