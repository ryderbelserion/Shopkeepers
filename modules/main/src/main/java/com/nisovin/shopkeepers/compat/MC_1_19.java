package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.19 upwards.
public final class MC_1_19 {

	public static final Optional<@NonNull Material> GOAT_HORN = Optional.ofNullable(
			CompatUtils.getMaterial("GOAT_HORN")
	);
	public static final Optional<@NonNull Material> MANGROVE_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("MANGROVE_SIGN")
	);
	public static final Optional<@NonNull Material> MANGROVE_WALL_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("MANGROVE_WALL_SIGN")
	);

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.19 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.19 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		return GOAT_HORN.isPresent();
	}

	private MC_1_19() {
	}
}
