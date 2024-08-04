package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import org.bukkit.Material;

import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.20 upwards.
public final class MC_1_20 {

	// New sign types:

	public static final Optional<Material> BAMBOO_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_SIGN")
	);
	public static final Optional<Material> BAMBOO_WALL_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_WALL_SIGN")
	);
	public static final Optional<Material> CHERRY_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_SIGN")
	);
	public static final Optional<Material> CHERRY_WALL_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_WALL_SIGN")
	);

	// Hanging signs:

	public static final Optional<Class<?>> HANGING_SIGN_BLOCK_DATA = Optional.ofNullable(
			ClassUtils.getClassOrNull("org.bukkit.block.data.type.HangingSign")
	);
	public static final Optional<Class<?>> WALL_HANGING_SIGN_BLOCK_DATA = Optional.ofNullable(
			ClassUtils.getClassOrNull("org.bukkit.block.data.type.WallHangingSign")
	);

	public static final Optional<Material> OAK_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("OAK_HANGING_SIGN")
	);
	public static final Optional<Material> OAK_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("OAK_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> SPRUCE_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("SPRUCE_HANGING_SIGN")
	);
	public static final Optional<Material> SPRUCE_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("SPRUCE_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> BIRCH_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BIRCH_HANGING_SIGN")
	);
	public static final Optional<Material> BIRCH_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BIRCH_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> JUNGLE_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("JUNGLE_HANGING_SIGN")
	);
	public static final Optional<Material> JUNGLE_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("JUNGLE_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> ACACIA_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("ACACIA_HANGING_SIGN")
	);
	public static final Optional<Material> ACACIA_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("ACACIA_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> DARK_OAK_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("DARK_OAK_HANGING_SIGN")
	);
	public static final Optional<Material> DARK_OAK_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("DARK_OAK_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> CRIMSON_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CRIMSON_HANGING_SIGN")
	);
	public static final Optional<Material> CRIMSON_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CRIMSON_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> WARPED_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("WARPED_HANGING_SIGN")
	);
	public static final Optional<Material> WARPED_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("WARPED_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> MANGROVE_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("MANGROVE_HANGING_SIGN")
	);
	public static final Optional<Material> MANGROVE_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("MANGROVE_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> BAMBOO_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_HANGING_SIGN")
	);
	public static final Optional<Material> BAMBOO_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("BAMBOO_WALL_HANGING_SIGN")
	);
	public static final Optional<Material> CHERRY_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_HANGING_SIGN")
	);
	public static final Optional<Material> CHERRY_WALL_HANGING_SIGN = Optional.ofNullable(
			CompatUtils.getMaterial("CHERRY_WALL_HANGING_SIGN")
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
