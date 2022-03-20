package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CompatUtils {

	public static @Nullable EntityType getEntityType(String name) {
		try {
			return EntityType.valueOf(name); // Not null
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static @Nullable Material getMaterial(String name) {
		try {
			return Material.valueOf(name); // Not null
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private CompatUtils() {
	}
}
