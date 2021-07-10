package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.17 upwards.
public class MC_1_17_Utils {

	public static final Material MATERIAL_GLOW_INK_SAC = CompatUtils.getMaterial("GLOW_INK_SAC");

	private MC_1_17_Utils() {
	}

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.17 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.17 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		return (MATERIAL_GLOW_INK_SAC != null);
	}
}
