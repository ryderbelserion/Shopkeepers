package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.compat.MC_1_19;
import com.nisovin.shopkeepers.compat.MC_1_20;
import com.nisovin.shopkeepers.util.java.Validate;

public enum SignType {

	// Previously persisted as 'GENERIC'.
	OAK(
			Material.OAK_SIGN,
			Material.OAK_WALL_SIGN,
			MC_1_20.OAK_HANGING_SIGN.orElse(null),
			MC_1_20.OAK_WALL_HANGING_SIGN.orElse(null)//
	),
	// Previously persisted as 'REDWOOD'.
	SPRUCE(
			Material.SPRUCE_SIGN,
			Material.SPRUCE_WALL_SIGN,
			MC_1_20.SPRUCE_HANGING_SIGN.orElse(null),
			MC_1_20.SPRUCE_WALL_HANGING_SIGN.orElse(null)//
	),
	BIRCH(
			Material.BIRCH_SIGN,
			Material.BIRCH_WALL_SIGN,
			MC_1_20.BIRCH_HANGING_SIGN.orElse(null),
			MC_1_20.BIRCH_WALL_HANGING_SIGN.orElse(null)//
	),
	JUNGLE(
			Material.JUNGLE_SIGN,
			Material.JUNGLE_WALL_SIGN,
			MC_1_20.JUNGLE_HANGING_SIGN.orElse(null),
			MC_1_20.JUNGLE_WALL_HANGING_SIGN.orElse(null)//
	),
	ACACIA(
			Material.ACACIA_SIGN,
			Material.ACACIA_WALL_SIGN,
			MC_1_20.ACACIA_HANGING_SIGN.orElse(null),
			MC_1_20.ACACIA_WALL_HANGING_SIGN.orElse(null)//
	),
	DARK_OAK(
			Material.DARK_OAK_SIGN,
			Material.DARK_OAK_WALL_SIGN,
			MC_1_20.DARK_OAK_HANGING_SIGN.orElse(null),
			MC_1_20.DARK_OAK_WALL_HANGING_SIGN.orElse(null)//
	),
	CRIMSON(
			Material.CRIMSON_SIGN,
			Material.CRIMSON_WALL_SIGN,
			MC_1_20.CRIMSON_HANGING_SIGN.orElse(null),
			MC_1_20.CRIMSON_WALL_HANGING_SIGN.orElse(null)//
	),
	WARPED(
			Material.WARPED_SIGN,
			Material.WARPED_WALL_SIGN,
			MC_1_20.WARPED_HANGING_SIGN.orElse(null),
			MC_1_20.WARPED_WALL_HANGING_SIGN.orElse(null)//
	),
	MANGROVE(
			MC_1_19.MANGROVE_SIGN.orElse(null),
			MC_1_19.MANGROVE_WALL_SIGN.orElse(null),
			MC_1_20.MANGROVE_HANGING_SIGN.orElse(null),
			MC_1_20.MANGROVE_WALL_HANGING_SIGN.orElse(null)//
	),
	BAMBOO(
			MC_1_20.BAMBOO_SIGN.orElse(null),
			MC_1_20.BAMBOO_WALL_SIGN.orElse(null),
			MC_1_20.BAMBOO_HANGING_SIGN.orElse(null),
			MC_1_20.BAMBOO_WALL_HANGING_SIGN.orElse(null)//
	),
	CHERRY(
			MC_1_20.CHERRY_SIGN.orElse(null),
			MC_1_20.CHERRY_WALL_SIGN.orElse(null),
			MC_1_20.CHERRY_HANGING_SIGN.orElse(null),
			MC_1_20.CHERRY_WALL_HANGING_SIGN.orElse(null)//
	);

	public static final Predicate<SignType> IS_SUPPORTED = SignType::isSupported;
	public static final Predicate<SignType> IS_HANGING_SUPPORTED = SignType::isHangingSupported;

	// These can be null if the current server version does not support the specific sign type.
	private final @Nullable Material signMaterial;
	private final @Nullable Material wallSignMaterial;
	private final @Nullable Material hangingSignMaterial;
	private final @Nullable Material wallHangingSignMaterial;

	private SignType(
			@Nullable Material signMaterial,
			@Nullable Material wallSignMaterial,
			@Nullable Material hangingSignMaterial,
			@Nullable Material wallHangingSignMaterial
	) {
		this.signMaterial = signMaterial;
		this.wallSignMaterial = wallSignMaterial;
		this.hangingSignMaterial = hangingSignMaterial;
		this.wallHangingSignMaterial = wallHangingSignMaterial;
		// Assert: Either both are null or none is non-null.
		assert signMaterial != null ^ wallSignMaterial == null;
		assert hangingSignMaterial != null ^ wallHangingSignMaterial == null;
	}

	public boolean isSupported() {
		return (signMaterial != null);
	}

	public boolean isHangingSupported() {
		return (hangingSignMaterial != null);
	}

	// Those getters are annotated as nullable so that callers are made aware that these methods
	// might throw an exception if the sign material is not available.
	public @Nullable Material getSignMaterial() {
		Validate.State.isTrue(this.isSupported(), "Unsupported sign type!");
		return signMaterial;
	}

	public @Nullable Material getWallSignMaterial() {
		Validate.State.isTrue(this.isSupported(), "Unsupported sign type!");
		return wallSignMaterial;
	}

	public @Nullable Material getSignMaterial(boolean wallSign) {
		return wallSign ? this.getWallSignMaterial() : this.getSignMaterial();
	}

	public @Nullable Material getHangingSignMaterial() {
		Validate.State.isTrue(this.isHangingSupported(), "Unsupported hanging sign type!");
		return hangingSignMaterial;
	}

	public @Nullable Material getWallHangingSignMaterial() {
		Validate.State.isTrue(this.isHangingSupported(), "Unsupported hanging sign type!");
		return wallHangingSignMaterial;
	}

	public @Nullable Material getHangingSignMaterial(boolean wallSign) {
		return wallSign ? this.getWallHangingSignMaterial() : this.getHangingSignMaterial();
	}
}
