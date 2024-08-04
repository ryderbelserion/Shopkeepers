package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.20.6 upwards.
public final class MC_1_20_6 {

	private static Optional<Boolean> IS_AVAILABLE = Optional.empty();

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.20.6 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.20.6 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		if (!IS_AVAILABLE.isPresent()) {
			boolean isAvailable = ClassUtils.getClassOrNull("org.bukkit.entity.Wolf$Variant") != null;
			IS_AVAILABLE = Optional.of(isAvailable);
		}
		return IS_AVAILABLE.get();
	}

	private MC_1_20_6() {
	}
}
