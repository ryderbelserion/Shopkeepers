package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.Log;

// TODO This can be removed once we only support Bukkit 1.16.1 upwards.
public class MC_1_16_Utils {

	private static final EntityType zombifiedPiglin = getEntityType("ZOMBIFIED_PIGLIN");
	private static final Material crimsonSign = getMaterial("CRIMSON_SIGN");
	private static final Material crimsonWallSign = getMaterial("CRIMSON_WALL_SIGN");
	private static final Material warpedSign = getMaterial("WARPED_SIGN");
	private static final Material warpedWallSign = getMaterial("WARPED_WALL_SIGN");

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

	private static EntityType getEntityType(String name) {
		try {
			EntityType entityType = EntityType.valueOf(name); // not null
			Log.debug(Settings.DebugOptions.capabilities, "Server knows EntityType '" + name + "'.");
			return entityType;
		} catch (IllegalArgumentException e) {
			Log.debug(Settings.DebugOptions.capabilities, "Server does not know EntityType '" + name + "'.");
			return null;
		}
	}

	private static Material getMaterial(String name) {
		try {
			Material material = Material.valueOf(name); // not null
			Log.debug(Settings.DebugOptions.capabilities, "Server knows Material '" + name + "'.");
			return material;
		} catch (IllegalArgumentException e) {
			Log.debug(Settings.DebugOptions.capabilities, "Server does not know Material '" + name + "'.");
			return null;
		}
	}
}
