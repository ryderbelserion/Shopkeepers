package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.16.1 upwards.
public final class MC_1_16 {

	public static final EntityType ZOMBIFIED_PIGLIN = CompatUtils.getEntityType("ZOMBIFIED_PIGLIN");
	public static final Material CRIMSON_SIGN = CompatUtils.getMaterial("CRIMSON_SIGN");
	public static final Material CRIMSON_WALL_SIGN = CompatUtils.getMaterial("CRIMSON_WALL_SIGN");
	public static final Material WARPED_SIGN = CompatUtils.getMaterial("WARPED_SIGN");
	public static final Material WARPED_WALL_SIGN = CompatUtils.getMaterial("WARPED_WALL_SIGN");

	public static void init() {
		if (ZOMBIFIED_PIGLIN != null) {
			Log.debug("MC 1.16 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.16 exclusive features are disabled.");
		}
	}

	private MC_1_16() {
	}
}
