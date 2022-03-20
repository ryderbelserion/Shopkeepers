package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.compat.MC_1_16;
import com.nisovin.shopkeepers.util.java.Validate;

public enum SignType {

	// Previously persisted as 'GENERIC'.
	OAK(Material.OAK_SIGN, Material.OAK_WALL_SIGN),
	// Previously persisted as 'REDWOOD'.
	SPRUCE(Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN),
	BIRCH(Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN),
	JUNGLE(Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN),
	ACACIA(Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN),
	DARK_OAK(Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN),
	CRIMSON(MC_1_16.CRIMSON_SIGN, MC_1_16.CRIMSON_WALL_SIGN),
	WARPED(MC_1_16.WARPED_SIGN, MC_1_16.WARPED_WALL_SIGN);

	public static final Predicate<SignType> IS_SUPPORTED = SignType::isSupported;

	// These can be null if the current server version does not support the specific sign type.
	private final @Nullable Material signMaterial;
	private final @Nullable Material wallSignMaterial;

	private SignType(@Nullable Material signMaterial, @Nullable Material wallSignMaterial) {
		this.signMaterial = signMaterial;
		this.wallSignMaterial = wallSignMaterial;
		// Assert: Either both are null or both are non-null.
		assert signMaterial != null ^ wallSignMaterial == null;
	}

	public boolean isSupported() {
		return (signMaterial != null);
	}

	// Annotated as nullable so that callers are made aware that this method might throw an
	// exception if the sign material is not available.
	public @Nullable Material getSignMaterial() {
		Validate.State.isTrue(this.isSupported(), "Unsupported sign type!");
		return signMaterial;
	}

	// Annotated as nullable so that callers are made aware that this method might throw an
	// exception if the sign material is not available.
	public @Nullable Material getWallSignMaterial() {
		Validate.State.isTrue(this.isSupported(), "Unsupported sign type!");
		return wallSignMaterial;
	}
}
