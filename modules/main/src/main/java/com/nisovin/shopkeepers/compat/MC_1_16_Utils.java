package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.16.1 upwards.
public class MC_1_16_Utils {

	private static final EntityType zombifiedPiglin = CompatUtils.getEntityType("ZOMBIFIED_PIGLIN");
	private static final Material crimsonSign = CompatUtils.getMaterial("CRIMSON_SIGN");
	private static final Material crimsonWallSign = CompatUtils.getMaterial("CRIMSON_WALL_SIGN");
	private static final Material warpedSign = CompatUtils.getMaterial("WARPED_SIGN");
	private static final Material warpedWallSign = CompatUtils.getMaterial("WARPED_WALL_SIGN");

	private MC_1_16_Utils() {
	}

	public static void init() {
		if (zombifiedPiglin != null) {
			Log.debug("MC 1.16 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.16 exclusive features are disabled.");
		}
	}

	public static EntityType getZombifiedPiglin() {
		return zombifiedPiglin;
	}

	public static Material getCrimsonSign() {
		return crimsonSign;
	}

	public static Material getCrimsonWallSign() {
		return crimsonWallSign;
	}

	public static Material getWarpedSign() {
		return warpedSign;
	}

	public static Material getWarpedWallSign() {
		return warpedWallSign;
	}
}
