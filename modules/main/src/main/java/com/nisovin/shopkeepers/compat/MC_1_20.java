package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.20 upwards.
public final class MC_1_20 {

	public static final Optional<@NonNull Material> BAMBOO_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_SIGN")
	);
	public static final Optional<@NonNull Material> BAMBOO_WALL_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_WALL_SIGN")
	);
	public static final Optional<@NonNull Material> CHERRY_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_SIGN")
	);
	public static final Optional<@NonNull Material> CHERRY_WALL_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_WALL_SIGN")
	);

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.20 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.20 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		return BAMBOO_SIGN.isPresent();
	}

	private MC_1_20() {
	}
}
