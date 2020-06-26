package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.util.Log;

// TODO This can be removed once we only support Bukkit 1.16.1 upwards.
public class MC_1_16_Utils {

	private static Optional<EntityType> zombifiedPiglin = null;

	private MC_1_16_Utils() {
	}

	public static EntityType getZombifiedPiglin() {
		if (zombifiedPiglin == null) {
			try {
				zombifiedPiglin = Optional.of(EntityType.valueOf("ZOMBIFIED_PIGLIN"));
				Log.debug("Server knows EntityType 'ZOMBIFIED_PIGLIN'.");
			} catch (IllegalArgumentException e) {
				zombifiedPiglin = Optional.empty();
				Log.debug("Server does not know EntityType 'ZOMBIFIED_PIGLIN'.");
			}
		}
		return zombifiedPiglin.orElse(null);
	}
}
