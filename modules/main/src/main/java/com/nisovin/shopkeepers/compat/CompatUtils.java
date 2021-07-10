package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.util.logging.Log;

class CompatUtils {

	private CompatUtils() {
	}

	public static EntityType getEntityType(String name) {
		try {
			EntityType entityType = EntityType.valueOf(name); // not null
			Log.debug(DebugOptions.capabilities, "Server knows EntityType '" + name + "'.");
			return entityType;
		} catch (IllegalArgumentException e) {
			Log.debug(DebugOptions.capabilities, "Server does not know EntityType '" + name + "'.");
			return null;
		}
	}

	public static Material getMaterial(String name) {
		try {
			Material material = Material.valueOf(name); // not null
			Log.debug(DebugOptions.capabilities, "Server knows Material '" + name + "'.");
			return material;
		} catch (IllegalArgumentException e) {
			Log.debug(DebugOptions.capabilities, "Server does not know Material '" + name + "'.");
			return null;
		}
	}
}
