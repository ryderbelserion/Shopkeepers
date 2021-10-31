package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

final class CompatUtils {

	public static EntityType getEntityType(String name) {
		try {
			return EntityType.valueOf(name); // Not null
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static Material getMaterial(String name) {
		try {
			return Material.valueOf(name); // Not null
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private CompatUtils() {
	}
}
